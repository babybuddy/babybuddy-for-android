package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import eu.pkgsoftware.babybuddywidgets.CredStore;

public class BabyBuddyClient extends StreamReader {
    private static Date parseNullOrDate(JSONObject o, String field) throws JSONException, ParseException {
        if (o.isNull(field)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

        // Remove milliseconds
        String strDate = o.getString(field);
        strDate = strDate.replaceAll("\\.[0-9]+([+Z])", "$1");
        strDate = strDate.replaceAll("Z$", "+00:00");
        return sdf.parse(strDate);
    }

    public static class Child {
        public int id;
        public String slug;
        public String first_name;
        public String last_name;
        public String birth_date;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Child child = (Child) o;
            return id == child.id &&
                Objects.equals(slug, child.slug) &&
                Objects.equals(first_name, child.first_name) &&
                Objects.equals(last_name, child.last_name) &&
                Objects.equals(birth_date, child.birth_date);
        }

        public static Child fromJSON(String s) throws JSONException {
            return fromJSON(new JSONObject(s));
        }

        public static Child fromJSON(JSONObject obj) throws JSONException {
            Child c = new Child();
            c.id = obj.getInt("id");
            c.slug = obj.getString("slug");
            c.first_name = obj.getString("first_name");
            c.last_name = obj.getString("last_name");
            c.birth_date = obj.getString("birth_date");
            return c;
        }

        public JSONObject toJSON() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("slug", slug);
                o.put("first_name", first_name);
                o.put("last_name", last_name);
                o.put("birth_date", birth_date);
            } catch (JSONException e) {
                throw new RuntimeException("ERROR should not happen");
            }
            return o;
        }
    }

    public static class Timer {
        public int id;
        public Integer child_id;
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

        public static Timer fromJSON(JSONObject obj) throws JSONException, ParseException {
            Timer t = new Timer();
            t.id = obj.getInt("id");
            t.child_id = obj.isNull("child") ? null : obj.getInt("child");
            t.name = obj.isNull("name") ? null : obj.getString("name");
            t.start = parseNullOrDate(obj, "start");
            t.end = parseNullOrDate(obj, "end");
            t.active = obj.getBoolean("active");
            t.user_id = obj.getInt("user");
            return t;
        }

        @Override
        public String toString() {
            return "Timer{" +
                "id=" + id +
                ", child_id=" + child_id +
                ", name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", active=" + active +
                ", user_id=" + user_id +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timer timer = (Timer) o;
            return id == timer.id && active == timer.active && user_id == timer.user_id && Objects.equals(child_id, timer.child_id) && Objects.equals(name, timer.name) && Objects.equals(start, timer.start) && Objects.equals(end, timer.end);
        }
    }

    public static class RequestCodeFailure extends IOException {
        public String response;

        public RequestCodeFailure(String response) {
            this.response = response;
        }
    }

    public interface RequestCallback<R> {
        void error(Exception error);
        void response(R response);
    }

    private final SimpleDateFormat SERVER_DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    private Handler syncMessage;
    private CredStore credStore;
    private Looper mainLoop;
    private long serverDateOffset = -1000;

    private void updateServerDateTime(HttpURLConnection con) {
        String dateString = con.getHeaderField("Date");
        try {
            Date serverTime = SERVER_DATE_FORMAT.parse(dateString);
            Date now = new Date(System.currentTimeMillis());

            serverDateOffset = serverTime.getTime() - now.getTime() - 100; // 100 ms offset
        } catch (ParseException e) {
        }
    }

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

    @NonNull
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date now = new Date(System.currentTimeMillis() + serverDateOffset);
        return sdf.format(now);
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
                        query.setRequestProperty("Content-Type", "application/json; utf-8");
                        query.setRequestProperty("Accept", "application/json");
                        OutputStream os = query.getOutputStream();
                        os.write(payload.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    query.connect();
                    updateServerDateTime(query);

                    int responseCode = query.getResponseCode();
                    if ((responseCode < 200) || (responseCode >= 300)) {
                        String message = query.getResponseMessage();
                        throw new RequestCodeFailure(message);
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
                    syncMessage.post(() -> callback.error(e));
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
                        result.add(Child.fromJSON(c));
                    }
                    callback.response(result.toArray(new Child[0]));
                } catch (JSONException e) {
                    this.error(e);
                }
            }
        });
    }

    public void listTimers(RequestCallback<Timer[]> callback) {
        listTimers(null, callback);
    }

    public void listTimers(Integer child_id, RequestCallback<Timer[]> callback) {
        String queryString = "?" + (child_id == null ? "" : ("child=" + child_id));
        dispatchQuery("GET", "api/timers/" + queryString, null, new RequestCallback<String>() {
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
                        result.add(Timer.fromJSON(item));
                    }
                    Collections.sort(
                        result,
                        new Comparator<Timer>() {
                            @Override
                            public int compare(Timer t1, Timer t2) {
                                return Integer.compare(t1.id, t2.id);
                            }
                        }
                    );
                    callback.response(result.toArray(new Timer[0]));
                } catch (JSONException | ParseException e) {
                    this.error(e);
                }
            }
        });
    }

    private static String urlencode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public void setTimerActive(int timer_id, boolean active, RequestCallback<Boolean> callback) {
        dispatchQuery(
                "PATCH",
                "api/timers/" + timer_id + (active ? "/restart/" : "/stop/"),
                null,
                new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                callback.response(true);
            }
        });
    }

    public void getTimer(int timer_id, RequestCallback<Timer> callback) {
        dispatchQuery(
                "GET",
                "api/timers/" + timer_id + "/",
                null,
                new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                JSONObject obj = null;
                try {
                    obj = new JSONObject(response);
                    callback.response( Timer.fromJSON(obj));
                } catch (JSONException | ParseException e) {
                    error(e);
                }
            }
        });
    }

    public void createSleepRecordFromTimer(Timer timer, String notes, RequestCallback<Boolean> callback) {
        dispatchQuery(
                "POST",
                "api/sleep/",
                "{\"timer\": XXTimer, \"notes\": \"XXNotes\"}"
                    .replaceAll("XXTimer", "" + timer.id)
                    .replaceAll("XXNotes", notes),
                new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                callback.response(true);
            }
        });
    }

    public void createTummyTimeRecordFromTimer(Timer timer, String notes, RequestCallback<Boolean> callback) {
        dispatchQuery(
                "POST",
                "api/tummy-times/",
                "{\"timer\": XXTimer, \"notes\": \"XXNotes\"}"
                    .replaceAll("XXTimer", "" + timer.id)
                    .replaceAll("XXNotes", notes),
                new RequestCallback<String>() {
            @Override
            public void error(Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                callback.response(true);
            }
        });
    }

    public void createFeedingRecordFromTimer(Timer timer, String type, String method, float amount, RequestCallback<Boolean> callback) {
        dispatchQuery(
            "POST",
            "api/feedings/",
            "{\"timer\": XXTimer, \"type\": \"XXType\", \"method\": \"XXMethod\", \"amount\": XXAmount, \"notes\": \"\"}"
                .replaceAll("XXTimer", "" + timer.id)
                .replaceAll("XXType", type)
                .replaceAll("XXMethod", method)
                .replaceAll("XXAmount", "" + amount),
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createChangeRecord(Child child, boolean wet, boolean solid, String notes, RequestCallback<Boolean> callback) {
        dispatchQuery(
            "POST",
            "api/changes/",
            "{\"child\": XXChild, \"time\": \"XXTime\", \"wet\": XXWet, \"solid\": XXSolid, \"color\": \"\", \"amount\": null, \"notes\": \"XXNotes\"}"
                .replaceAll("XXChild", "" + child.id)
                .replaceAll("XXTime", now())
                .replaceAll("XXWet", wet ? "true" : "false")
                .replaceAll("XXSolid", solid ? "true" : "false")
                .replaceAll("XXNotes", notes),
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void createTimer(Child child, String name, RequestCallback<Timer> callback) {
        dispatchQuery(
            "POST",
            "api/timers/",
            "{\"child\": XXChild, \"name\": \"XXName\", \"start\": \"XXStart\"}"
                .replaceAll("XXChild", "" + child.id)
                .replaceAll("XXStart", now())
                .replaceAll("XXName", name),
            new RequestCallback<String>() {
                @Override
                public void error(Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    try {
                        callback.response( Timer.fromJSON(new JSONObject(response)));
                    } catch (JSONException | ParseException e) {
                        error(e);
                    }
                }
            }
        );
    }

    public long getServerDateOffsetMillis() {
        return serverDateOffset;
    }
}
