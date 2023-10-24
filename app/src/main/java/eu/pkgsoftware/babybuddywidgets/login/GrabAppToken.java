package eu.pkgsoftware.babybuddywidgets.login;

/*
Login protocol in a nutshell:

Login transaction

- Get /login/
    - Copy cookie header: Set-Cookie: csrftoken=...;
    - Copy csrfmiddlewaretoken: <input type="hidden" name="csrfmiddlewaretoken" value="...">
- Post to /login/
    -d username=""
    -d password=""
    -d csrfmiddlewaretoken=...
    --cookie csrftoken=...

Answer:
    403 code - incorrect login
    200 - copy session id, we are done!


When logged in:
Browse to: http://192.168.0.3:18000/user/settings/
Search HTML for "api_key_regenerate", <div>, then find the first "code like"
text node before that input.
 */

import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.pkgsoftware.babybuddywidgets.networking.StreamReader;
import eu.pkgsoftware.babybuddywidgets.networking.UserFormInteractions;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Profile;

public class GrabAppToken extends StreamReader {
    public static class MissingPage extends Exception {
    }

    private static class ThreadResult {
        public Exception error = null;
        public String result = null;
    }

    /**
     * Attempts to login to Baby Buddy with the given username/password combination and obtain the
     * app-key.
     *
     * @param url
     * @param username
     * @param password
     * @return Returns the token on success, or null on failure.
     */
    public static String grabToken(String url, String username, String password) throws Exception {
        // Screw you android! I run my network-stuff synchronous if _I_ want to!
        ThreadResult threadResult = new ThreadResult();
        Thread thread = new Thread() {
            @Override
            public void run() {
                String key = null;
                try {
                    GrabAppToken gat = new GrabAppToken(new URL(url));
                    gat.login(username, password);
                    try {
                        key = gat.getFromProfilePage();
                    } catch (MissingPage e) {
                        key = gat.parseFromSettingsPage();
                    }
                } catch (IOException e) {
                    threadResult.error = e;
                } catch (Exception e) {
                    e.printStackTrace();
                    threadResult.error = e;
                }
                threadResult.result = key;
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (threadResult.error != null) {
            throw threadResult.error;
        }
        return threadResult.result;
    }


    private URL url;
    private HashMap<String, String> headers;
    private HashMap<String, String> cookies;

    public GrabAppToken(URL url) {
        this.url = url;
        this.headers = new HashMap<String, String>();
        this.headers.put("charset", "utf-8");
        this.cookies = new HashMap<String, String>();
    }

    private URL subPath(String path) throws MalformedURLException {
        String prefix = url.toString().replaceAll("/*$", "");
        return new URL(prefix + "/" + path);
    }

    private HttpURLConnection doQuery(String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) subPath(path).openConnection();
        for (Map.Entry<String, String> item : headers.entrySet()) {
            con.setRequestProperty(item.getKey(), item.getValue());
        }
        StringBuilder cookieString = new StringBuilder();
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            if (cookieString.length() > 0) {
                cookieString.append("; ");
            }
            cookieString.append(cookie.getKey());
            cookieString.append("=");
            cookieString.append(URLEncoder.encode(cookie.getValue(), StandardCharsets.UTF_8.toString()));
        }
        if (cookieString.length() > 0) {
            con.setRequestProperty("Cookie", cookieString.toString());
        }
        con.setDoInput(true);
        return con;
    }

    private String urlEncodedPostString(Map<String, String> values) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> kv : values.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append(URLEncoder.encode(kv.getKey(), StandardCharsets.UTF_8.toString()));
            result.append("=");
            result.append(URLEncoder.encode(kv.getValue(), StandardCharsets.UTF_8.toString()));
        }
        return result.toString();
    }

    public void login(String username, String password) throws IOException {
        HttpURLConnection con = doQuery("login/");
        if (con.getResponseCode() != 200) {
            throw new IOException("Baby Buddy login/ response not 200");
        }

        // Find the CSRF token
        Map<String, List<String>> headers = con.getHeaderFields();
        String csrftoken = null;
        if (headers.containsKey("Set-Cookie")) {
            List<String> cookieItems = headers.get("Set-Cookie");
            if (cookieItems != null) {
                for (String item : cookieItems) {
                    if (item.startsWith("csrftoken=")) {
                        csrftoken = item.split("=", 2)[1].split(";")[0];
                    }
                }
            }
        }
        if (csrftoken == null) {
            throw new IOException("csrftoken missing");
        }

        // Load the html - max 1 MB
        String html = loadHttpData(con);

        // Find: <input type="hidden" name="csrfmiddlewaretoken" value="...">
        String csrfmiddlewaretoken = UserFormInteractions.extractCsrfmiddlewaretoken(html);

        // Perform the login
        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("csrfmiddlewaretoken", csrfmiddlewaretoken);
        postData.put("username", username);
        postData.put("password", password);
        postData.put("next", "/");
        String urlEncodedString = urlEncodedPostString(postData);


        final String referer = con.getURL().toString();

        con = doQuery("login/");
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);

        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Cookie", "csrftoken=" + csrftoken);
        con.setRequestProperty("Content-Length", "" + urlEncodedString.length());
        con.setRequestProperty("Referer", referer);

        OutputStream os = con.getOutputStream();
        os.write(urlEncodedString.getBytes(StandardCharsets.UTF_8));
        os.flush();

        int repCode = con.getResponseCode();
        if (repCode == 403) {
            String errorMessage = loadHttpData(con.getErrorStream());
            if (checkPresentableMessage(errorMessage)) {
                throw new IOException(errorMessage);
            }
            throw new IOException("Invalid username or password (403)");
        }
        if ((repCode != 200) && ((repCode < 300) || (repCode > 307))) {
            throw new IOException("Login failed for unknown reason (server issue?)");
        }

        String sessionid = null;
        headers = con.getHeaderFields();
        if (headers.containsKey("Set-Cookie")) {
            List<String> cookieItems = headers.get("Set-Cookie");
            if (cookieItems != null) {
                for (String item : cookieItems) {
                    if (item.startsWith("sessionid=")) {
                        sessionid = item.split("=", 2)[1].split(";")[0];
                    }
                }
            }
        }
        if (sessionid == null) {
            // Try to parse out the HTML-embedded error message
            String errorMessage = parseOutAlertPillMessage(loadHttpData(con));
            if ((errorMessage != null) && checkPresentableMessage(errorMessage)) {
                throw new IOException(errorMessage);
            }

            throw new IOException("Invalid username or password (sessionid)");
        }

        // Login succeeded - store the session id!
        cookies.put("sessionid", sessionid);
    }

    private boolean checkPresentableMessage(String errorMessage) {
        return !(
            errorMessage.contains("<") && errorMessage.contains(">")
                || errorMessage.contains("\n")
                || errorMessage.length() > 256
        );
    }

    public String getFromProfilePage() throws MissingPage, IOException {
        HttpURLConnection con = doQuery("api/profile");
        con.setRequestProperty("Accept", "application/json");
        if (con.getResponseCode() == 404) {
            throw new MissingPage();
        }
        if (con.getResponseCode() != 200) {
            throw new IOException("Cannot access profile page");
        }

        String json = loadHttpData(con);
        ObjectMapper mapper = new ObjectMapper();
        Profile profile = mapper.readValue(json, Profile.class);
        return profile.getApiKey();
    }

    public String parseFromSettingsPage() throws IOException {
        HttpURLConnection con = doQuery("user/settings/");
        if (con.getResponseCode() != 200) {
            throw new IOException("Cannot access user settings");
        }

        String html = loadHttpData(con);
        String flatHtml = html.replace("\n", "").replace("\r", "");

        Pattern pat = Pattern.compile("<div[^>]*>(.*)<input.*name=.api_key_regenerate");
        Matcher match = pat.matcher(flatHtml);
        if (!match.find()) {
            throw new IOException("Cannot find api-key section");
        }

        String keySection = match.group(1);
        if (keySection == null) {
            throw new IOException("Cannot find api-key section");
        }
        int divIndex = keySection.lastIndexOf("<div");
        if (divIndex >= 0) {
            keySection = keySection.substring(divIndex);
        }
        String[] splits = keySection.replaceAll("<[^>]+>", " ").trim().split(" ");
        return splits[splits.length - 1];
    }

    private String parseOutAlertPillMessage(String html) {
        // This:
        // <div class="alert alert-danger" role="alert">
        //    <strong>Error:</strong> Please enter a correct username and password. Note that both fields may be case-sensitive.
        // </div>
        Pattern findDiv = Pattern.compile(
            "<div[^>]* role=\"alert\"[^>]*>(.*?)</div>",
            Pattern.DOTALL | Pattern.MULTILINE
        );
        Matcher m = findDiv.matcher(html);
        if (!m.find()) {
            return null;
        }

        String errorMessage = m.group(1);
        if (errorMessage == null) {
            return null;
        }

        Pattern htmlTag = Pattern.compile("<([a-zA-Z-_]+)[^>]*>", Pattern.DOTALL | Pattern.MULTILINE);
        while (true) {
            m = htmlTag.matcher(errorMessage);
            if (!m.find()) {
                break;
            }

            String tagName = m.group(1);
            if (tagName == null) {
                break;
            }
            String tagEnd = "</NAME>".replace("NAME", tagName);
            int last = errorMessage.indexOf(tagEnd, m.end());
            if (last >= 0) {
                last += tagEnd.length();
                errorMessage = errorMessage.substring(0, m.start()) + errorMessage.substring(last);
            } else {
                errorMessage = errorMessage.substring(0, m.start()) + errorMessage.substring(m.end() + 1);
            }
        }

        return errorMessage.trim();
    }

}