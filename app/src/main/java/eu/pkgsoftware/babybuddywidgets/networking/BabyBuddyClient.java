package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;

import androidx.annotation.NonNull;
import eu.pkgsoftware.babybuddywidgets.Constants;
import eu.pkgsoftware.babybuddywidgets.CredStore;
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.Client;

public class BabyBuddyClient extends StreamReader {
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX";

    public static class ACTIVITIES {
        public static final String SLEEP = "sleep";
        public static final String TUMMY_TIME = "tummy-times";
        public static final String FEEDING = "feedings";
        public static final String PUMPING = "pumping";

        public static final String[] ALL = new String[4];

        static {
            ALL[0] = FEEDING;
            ALL[1] = SLEEP;
            ALL[2] = TUMMY_TIME;
            ALL[3] = PUMPING;
        }

        public static int index(String s) {
            for (int i = 0; i < ALL.length; i++) {
                if (Objects.equals(ALL[i], s)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class EVENTS {
        public static final String CHANGE = "changes";
        public static final String NOTE = "notes";
        public static final String TEMPERATURE = "temperature";
        public static final String WEIGHT = "weight";
        public static final String BMI = "bmi";
        public static final String HEIGHT = "height";
        public static final String HEAD_CIRCUMFERENCE = "head-circumference";

        public static final String[] ALL = new String[7];

        static {
            ALL[0] = CHANGE;
            ALL[1] = NOTE;
            ALL[2] = TEMPERATURE;
            ALL[3] = WEIGHT;
            ALL[4] = BMI;
            ALL[5] = HEIGHT;
            ALL[6] = HEAD_CIRCUMFERENCE;
        }

        public static int index(String s) {
            for (int i = 0; i < ALL.length; i++) {
                if (Objects.equals(ALL[i], s)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static Date parseNullOrDate(JSONObject o, String field) throws JSONException, ParseException {
        if (o.isNull(field)) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);

        // Remove milliseconds
        String strDate = o.getString(field);
        strDate = strDate.replaceAll("\\.[0-9]+([+-Z])", "$1");
        strDate = strDate.replaceAll("Z$", "+00:00");
        return sdf.parse(strDate);
    }

    private static String dateToString(Date date) {
        if (date == null) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
        return sdf.format(date);
    }

    private static String dateToQueryString(Date date) {
        if (date == null) {
            return null;
        }
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public static class QueryValues {
        public HashMap<String, String> queryValues = new HashMap<>();

        public QueryValues add(String name, String value) {
            queryValues.put(name, value);
            return this;
        }

        public QueryValues add(String name, int value) {
            return this.add(name, "" + value);
        }

        public QueryValues add(String name, Date value) {
            return this.add(name, dateToQueryString(value));
        }

        public String toQueryString() {
            StringBuilder result = new StringBuilder("");
            for (Map.Entry<String, String> e : queryValues.entrySet()) {
                if (result.length() > 0) {
                    result.append("&");
                }
                result.append(e.getKey());
                result.append("=");
                result.append(urlencode(e.getValue()));
            }
            return result.toString();
        }

        public String toCookiesString() {
            StringBuilder result = new StringBuilder("");
            for (Map.Entry<String, String> e : queryValues.entrySet()) {
                if (result.length() > 0) {
                    result.append("; ");
                }
                result.append(e.getKey());
                result.append("=");
                result.append(e.getValue());
            }
            return result.toString();
        }

        public JSONObject toJsonObject() {
            return new JSONObject(queryValues);
        }
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

        public @NonNull String readableName() {
            if (name != null) {
                return name;
            }
            return "Quick timer #" + id;
        }

        public Date computeCurrentServerEndTime(BabyBuddyClient client) {
            if (end != null) {
                return end;
            }
            long serverMillis = new Date().getTime() + client.getServerDateOffsetMillis();
            return new Date(serverMillis);
        }

        public static Timer fromJSON(JSONObject obj) throws JSONException, ParseException {
            Timer t = new Timer();
            t.id = obj.getInt("id");
            t.child_id = obj.isNull("child") ? null : obj.getInt("child");
            t.name = obj.isNull("name") ? null : obj.getString("name");
            t.start = parseNullOrDate(obj, "start");
            t.end = parseNullOrDate(obj, "end");
            // Starting at v2.0 of baby buddy, the active-field is gone. Timers are always active when present!
            t.active = obj.optBoolean("active", true);
            t.user_id = obj.getInt("user");
            return t;
        }

        public JSONObject toJSON() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("child", child_id);
                o.put("name", name);
                o.put("start", dateToString(start));
                o.put("end", dateToString(end));
                o.put("active", active);
                o.put("user", user_id);
            } catch (JSONException e) {
                throw new RuntimeException("ERROR should not happen");
            }
            return o;
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

        public Timer clone() {
            Timer result = new Timer();
            result.id = id;
            result.child_id = child_id;
            result.name = name;
            result.start = start;
            result.end = end;
            result.active = active;
            result.user_id = user_id;
            return result;
        }
    }

    public static class TimeEntry {
        public String type;
        public int typeId;
        public Date start;
        public Date end;
        public String notes;

        public static TimeEntry fromJsonObject(JSONObject json, String type) throws JSONException, ParseException {
            String notes = null;
            if (json.has("milestone")) {
                notes = json.getString("milestone");
            }
            if (json.has("notes")) {
                notes = json.getString("notes");
            }
            return new TimeEntry(
                type,
                json.getInt("id"),
                parseNullOrDate(json, "start"),
                parseNullOrDate(json, "end"),
                notes == null ? "" : notes
            );
        }

        public TimeEntry(String type, int typeId, Date start, Date end, String notes) {
            this.type = type;
            this.typeId = typeId;
            this.start = start;
            this.end = end;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return "TimeEntry{" +
                "type='" + type + '\'' +
                ", typeId=" + typeId +
                ", start=" + start +
                ", end=" + end +
                ", notes='" + notes + '\'' +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeEntry timeEntry = (TimeEntry) o;
            return typeId == timeEntry.typeId &&
                Objects.equals(type, timeEntry.type) &&
                Objects.equals(start, timeEntry.start) &&
                Objects.equals(end, timeEntry.end) &&
                Objects.equals(notes, timeEntry.notes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, typeId, start, end, notes);
        }
    }

    public class GenericSubsetResponseHeader<T> {
        public final int offset;
        public final int totalCount;
        public final T payload;

        public GenericSubsetResponseHeader(int offset, int totalCount, T payload) {
            this.offset = offset;
            this.totalCount = totalCount;
            this.payload = payload;
        }
    }

    public class GenericListSubsetResponse<LT> {
        public final int offset;
        public final int totalCount;
        public final LT[] list;

        public GenericListSubsetResponse(int offset, int totalCount, LT[] list) {
            this.offset = offset;
            this.totalCount = totalCount;
            this.list = list;
        }
    }

    public interface RequestCallback<R> {
        void error(@NotNull Exception error);

        void response(R response);
    }

    private final SimpleDateFormat SERVER_DATE_FORMAT = new SimpleDateFormat(
        "EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH
    );

    private Handler syncMessage;
    private CredStore credStore;
    private Looper mainLoop;
    private long serverDateOffset = -1000;

    public final Client v2client;

    private void updateServerDateTime(HttpURLConnection con) {
        String dateString = con.getHeaderField("Date");
        if (dateString == null) {
            GlobalDebugObject.log("updateServerDateTime(): Date header not found");
            return; // Chicken out, no dateString found, let's hope everything works!
        }
        try {
            Date serverTime = SERVER_DATE_FORMAT.parse(dateString);
            Date now = new Date(System.currentTimeMillis());

            serverDateOffset = serverTime.getTime() - now.getTime() - 100; // 100 ms offset
            GlobalDebugObject.log("updateServerDateTime(): Adjusted serverDateOffset to " + serverDateOffset);
        } catch (ParseException e) {
            GlobalDebugObject.log("updateServerDateTime(): Date header parse error; " + e);
        }
    }

    public URL pathToUrl(String path) throws MalformedURLException {
        String prefix = credStore.getServerUrl().replaceAll("/*$", "");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new URL(prefix + "/" + path);
    }

    private HttpURLConnection doQuery(String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) pathToUrl(path).openConnection();
        String token = credStore.getAppToken();
        con.setRequestProperty("Authorization", "Token " + token);

        QueryValues qValues = new QueryValues();
        for (Map.Entry<String, String> entry : credStore.getAuthCookies().entrySet()) {
            qValues.add(entry.getKey(), entry.getValue());
        }
        con.setRequestProperty("Cookie", qValues.toCookiesString());

        con.setDoInput(true);
        return con;
    }

    @NonNull
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH
        );
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date now = new Date(System.currentTimeMillis() + serverDateOffset);
        return sdf.format(now);
    }

    public BabyBuddyClient(Looper mainLoop, CredStore credStore) {
        this.mainLoop = mainLoop;
        this.credStore = credStore;
        this.syncMessage = new Handler(mainLoop);
        this.v2client = new Client(credStore);
    }

    private final Random requestIdGenerator = new Random();
    private synchronized int newRequestId() {
        return Math.abs(requestIdGenerator.nextInt()) % 10000;
    }

    private void dispatchQuery(String method, String path, String payload, RequestCallback<String> callback) {
        final int REQUEST_ID = newRequestId();
        final String QUERY_STR = "Query " + REQUEST_ID;

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection query = doQuery(path);
                    GlobalDebugObject.log(QUERY_STR + ": " + method + " to " + path + "; payload = " + payload);

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
                        InputStream errorStream = query.getErrorStream();
                        String messageText;
                        if (errorStream == null) {
                            messageText = "[no message]";
                        } else {
                            messageText = loadHttpData(errorStream);
                        }
                        GlobalDebugObject.log(
                            QUERY_STR + " response error: " + responseCode + "; messageText = " + messageText
                        );
                        throw new RequestCodeFailure(responseCode, message, messageText);
                    }


                    final String result = loadHttpData(query);
                    GlobalDebugObject.log(QUERY_STR + " succeeded: response = " + result);
                    syncMessage.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.response(result);
                        }
                    });
                } catch (Exception e) {
                    GlobalDebugObject.log(QUERY_STR + ": Exception occurred; " + e);
                    syncMessage.post(() -> callback.error(e));
                }
            }
        };
        thread.start();
    }

    public void listChildren(RequestCallback<Child[]> callback) {
        listChildren(new QueryValues(), callback);
    }

    public void listChildren(@NotNull QueryValues queryValues, RequestCallback<Child[]> callback) {
        queryValues.add("limit", 1000000);
        dispatchQuery(
            "GET",
            "api/children/?" + queryValues.toQueryString(),
            null,
            new RequestCallback<String>() {
                @Override
                public void error(@NonNull Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    try {
                        JSONObject obj = new JSONObject(response);
                        JSONArray children = obj.getJSONArray("results");
                        List<Child> result = new ArrayList<>(children.length());
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

    public void checkChildExists(int child_id, RequestCallback<Boolean> callback) {
        listChildren(
            new QueryValues().add("id", child_id),
            new RequestCallback<Child[]>() {
                @Override
                public void error(@NonNull Exception error) {
                    callback.error(error);
                }

                @Override
                public void response(Child[] response) {
                    for (Child c : response) {
                        if (c.id == child_id) {
                            callback.response(true);
                        }
                    }
                    callback.response(false);
                }
            }
        );
    }

    public void createTimer(int child_id, String name, RequestCallback<Timer> callback) {
        String data;
        try {
            data = (new JSONObject())
                .put("child", child_id)
                .put("name", name)
                .put("start", now())
                .toString();
        } catch (JSONException e) {
            throw new RuntimeException("JSON Structure not built correctly");
        }

        dispatchQuery(
            "POST",
            "api/timers/",
            data,
            new RequestCallback<String>() {
                @Override
                public void error(@NonNull Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    Timer result;
                    try {
                        result = Timer.fromJSON(new JSONObject(response));
                    } catch (JSONException | ParseException e) {
                        error(e);
                        return;
                    }
                    callback.response(result);
                }
            }
        );
    }

    public void listTimers(RequestCallback<Timer[]> callback) {
        listTimers(null, callback);
    }

    public void listTimers(Integer child_id, RequestCallback<Timer[]> callback) {
        final QueryValues qv = new QueryValues();
        if (child_id != null) {
            qv.add("child", child_id);
        }
        qv.add("limit", 1000000);
        dispatchQuery("GET", "api/timers/?" + qv.toQueryString(), null, new RequestCallback<String>() {
            @Override
            public void error(@NonNull Exception e) {
                callback.error(e);
            }

            @Override
            public void response(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray timers = obj.getJSONArray("results");
                    List<Timer> result = new ArrayList<>(timers.length());
                    for (int i = 0; i < timers.length(); i++) {
                        JSONObject item = timers.getJSONObject(i);
                        result.add(Timer.fromJSON(item));
                    }
                    result.sort(Comparator.comparingInt(t -> t.id));
                    callback.response(result.toArray(new Timer[0]));
                } catch (JSONException | ParseException e) {
                    this.error(e);
                }
            }
        });
    }

    public void deleteTimer(int timer_id, RequestCallback<Boolean> callback) {
        simpleCall("DELETE", String.format(Locale.US, "api/timers/%d/", timer_id), null, callback);
    }

    private static String urlencode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public void restartTimer(int timer_id, RequestCallback<Timer> callback) {
        dispatchQuery(
            "PATCH",
            "api/timers/" + timer_id + "/restart/",
            null,
            new RequestCallback<>() {
                @Override
                public void error(@NonNull Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    try {
                        JSONObject obj = new JSONObject(response);
                        callback.response(Timer.fromJSON(obj));
                    } catch (JSONException | ParseException e) {
                        this.error(e);
                    }
                }
            });
    }

    public long getServerDateOffsetMillis() {
        return serverDateOffset;
    }

    @NonNull
    private static String addQueryParameters(QueryValues queryValues, @NonNull String path) {
        if (queryValues == null) {
            queryValues = new QueryValues();
        }
        path = path + "?" + queryValues.toQueryString();
        return path;
    }

    private void simpleCall(String method, String path, QueryValues query, RequestCallback<Boolean> callback) {
        path = addQueryParameters(query, path);
        dispatchQuery(
            method,
            path,
            null,
            new RequestCallback<>() {
                @Override
                public void error(@NonNull Exception e) {
                    callback.error(e);
                }

                @Override
                public void response(String response) {
                    callback.response(true);
                }
            });
    }

    public void listGeneric(
        String activity,
        int offset,
        QueryValues queryValues,
        RequestCallback<GenericSubsetResponseHeader<JSONArray>> callback
    ) {
        String path = "api/" + activity + "/";
        if (queryValues == null) {
            queryValues = new QueryValues();
        }
        queryValues.add("offset", offset);
        path = addQueryParameters(queryValues, path);
        dispatchQuery("GET", path, null, new RequestCallback<String>() {
            @Override
            public void error(@NonNull Exception error) {
                callback.error(error);
            }

            @Override
            public void response(String response) {
                JSONArray result = null;
                int totalCount = 0;
                try {
                    JSONObject listResponse = new JSONObject(response);
                    totalCount = listResponse.getInt("count");
                    result = listResponse.getJSONArray("results");
                } catch (JSONException e) {
                    error(e);
                    return;
                }
                callback.response(new GenericSubsetResponseHeader<>(offset, totalCount, result));
            }
        });
    }

    private interface WrapTimelineEntry<TE extends TimeEntry> {
        TE wrap(JSONObject json) throws ParseException, JSONException;
    }

    public void updateTimelineEntry(
        @NotNull TimeEntry entry,
        @NotNull QueryValues values,
        @NotNull RequestCallback<TimeEntry> callback
    ) {
        String path = "api/" + entry.type + "/" + entry.typeId + "/";
        dispatchQuery(
            "PATCH",
            path,
            values.toJsonObject().toString(),
            new RequestCallback<String>() {
                @Override
                public void error(@NonNull Exception error) {
                    callback.error(error);
                }

                @Override
                public void response(String response) {
                    try {
                        JSONObject o = new JSONObject(response);
                        callback.response(TimeEntry.fromJsonObject(o, entry.type));
                    } catch (JSONException | ParseException e) {
                        callback.error(e);
                    }
                }
            }
        );
    }
}
