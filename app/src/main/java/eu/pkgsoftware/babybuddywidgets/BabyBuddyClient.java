package eu.pkgsoftware.babybuddywidgets;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class BabyBuddyClient extends StreamReader {
    public static class Child {
        public int id;
        public String slug;
        public String first_name;
        public String last_name;
        public String birth_date;
    }

    public static class Timer {
        public int id;
        public int child_id;
        public String name;
        public Date start;
        public Date end;
        public boolean active;
        public int user_id;

        public String readableName() {
            if (name != null) {
                return name;
            }
            return "Quick timer #" + id;
        }
    }

    public static class RequestCodeFailure extends IOException {}

    public interface RequestCallback<R> {
        void error(Exception error);
        void response(R response);
    }

    private static class MessageData {
        public String data = null;
        public Exception error = null;

        public MessageData(String data) {
            this.data = data;
        }

        public MessageData(Exception error) {
            this.error = error;
        }
    }

    private Handler syncMessage;
    private CredStore credStore;
    private Looper mainLoop;

    private URL subPath(String path) throws MalformedURLException {
        String prefix = credStore.getServerUrl().replaceAll("/*$", "");
        return new URL(prefix + "/" + path);
    }

    private HttpURLConnection doQuery(String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) subPath(path).openConnection();
        String token = credStore.getAppToken();
        con.setRequestProperty("Authorization", "Token " + token);
        con.setDoInput(true);
        return con;
    }

    public BabyBuddyClient(Looper mainLoop, CredStore credStore) {
        this.mainLoop = mainLoop;
        this.credStore = credStore;

        this.syncMessage = new Handler(mainLoop);
    }

    private void dispatchQuery(String method, String path, String payload, RequestCallback<String> callback) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection query = doQuery(path);
                    query.setRequestMethod(method);
                    if (payload != null) {
                        query.setDoOutput(true);
                        OutputStream os = query.getOutputStream();
                        os.write(payload.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    if (query.getResponseCode() != 200) {
                        throw new RequestCodeFailure();
                    }


                    String result = loadHtml(query);
                    syncMessage.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.response(result);
                        }
                    });
                }
                catch (Exception e) {
                    syncMessage.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.error(e);
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void listChildren(RequestCallback<Child[]> callback) {
        dispatchQuery("GET", "api/children/", null, new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray children = obj.getJSONArray("results");
                    List<Child> result = new ArrayList<Child>(children.length());
                    for (int i = 0; i < children.length(); i++) {
                        JSONObject c = children.getJSONObject(i);
                        Child child = new Child();
                        child.id = c.getInt("id");
                        child.slug = c.getString("slug");
                        child.first_name = c.getString("first_name");
                        child.last_name = c.getString("last_name");
                        child.birth_date = c.getString("birth_date");
                        result.add(child);
                    }
                    callback.response(result.toArray(new Child[0]));
                } catch (JSONException e) {
                    this.error(e);
                }
            }
        });
    }

    private static Date parseNullOrDate(JSONObject o, String field) throws JSONException, ParseException {
        if (o.isNull(field)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String strDate = o.getString(field);
        strDate = strDate.split(Pattern.quote("+"))[0];
        strDate = strDate.split(Pattern.quote("."))[0];
        return sdf.parse(strDate);
    }

    public void listTimers(RequestCallback<Timer[]> callback) {
        dispatchQuery("GET", "api/timers/", null, new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray timers = obj.getJSONArray("results");
                    List<Timer> result = new ArrayList<Timer>(timers.length());
                    for (int i = 0; i < timers.length(); i++) {
                        JSONObject item = timers.getJSONObject(i);
                        Timer timer = new Timer();
                        timer.id = item.getInt("id");
                        timer.child_id = item.getInt("child");
                        timer.name = item.isNull("name") ? null : item.getString("name");
                        timer.start = parseNullOrDate(item, "start");
                        timer.end = parseNullOrDate(item, "end");
                        timer.active = item.getBoolean("active");
                        timer.user_id = item.getInt("user");
                        result.add(timer);
                    }
                    callback.response(result.toArray(new Timer[0]));
                } catch (JSONException | ParseException e) {
                    this.error(e);
                }
            }
        });
    }
}
