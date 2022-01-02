package eu.pkgsoftware.babybuddywidgets;

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

public class GrabAppToken {
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
     * @return
     * Returns the token on success, or null on failure.
     */
    public static String grabToken(String url, String username, String password) {
        // Screw you android! I run my network-stuff synchronous if _I_ want to!
        ThreadResult threadResult = new ThreadResult();
        Thread thread = new Thread() {
            @Override
            public void run() {
                String key = null;
                try {
                    GrabAppToken gat = new GrabAppToken(new URL(url));
                    gat.login(username, password);
                    key = gat.obtainAppKey();
                } catch (IOException e) {
                    threadResult.error = e;
                }
                catch (Exception e) {
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
            System.out.println("Baby Buddy login error: " + threadResult.error);
        }
        return threadResult.result;
    }


    private URL url;
    private HashMap<String, String> headers;
    private HashMap<String, String> cookies;

    private GrabAppToken(URL url) {
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

    private void login(String username, String password) throws IOException {
        HttpURLConnection con = doQuery("login/");
        if (con.getResponseCode() != 200) {
            throw new IOException("Baby Buddy login/ response not 200");
        }

        // Find the CSRF token
        Map<String, List<String>> headers = con.getHeaderFields();
        String csrftoken = null;
        if (headers.containsKey("Set-Cookie")) {
            for (String item : headers.get("Set-Cookie")) {
                if (item.startsWith("csrftoken=")) {
                    csrftoken = item.split("=", 2)[1].split(";")[0];
                }
            }
        }
        if (csrftoken == null) {
            throw new IOException("csrftoken missing");
        }

        // Load the html - max 1 MB
        String html = loadHtml(con);;

        // Find: <input type="hidden" name="csrfmiddlewaretoken" value="...">
        String match;
        {
            Pattern pat = Pattern.compile("<input .*name=\"csrfmiddlewaretoken\".*>");
            Matcher m = pat.matcher(html);
            if (!m.find()) {
                throw new IOException("csrfmiddlewaretoken missing");
            }
            match = m.group(0);
        }
        String csrfmiddlewaretoken = match.split("value=")[1].split("\"")[1];

        // Perform the login
        HashMap<String, String> postData = new HashMap<String, String>();
        postData.put("csrfmiddlewaretoken", csrfmiddlewaretoken);
        postData.put("username", username);
        postData.put("password", password);
        String urlEncodedString =  urlEncodedPostString(postData);


        con = doQuery("login/");
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);

        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Cookie", "csrftoken=" + csrftoken);
        con.setRequestProperty("Content-Length", "" + urlEncodedString.length());

        OutputStream os = con.getOutputStream();
        os.write(urlEncodedString.getBytes(StandardCharsets.UTF_8));
        os.flush();

        int repCode = con.getResponseCode();
        if (repCode == 403) {
            throw new IOException("Invalid username or password");
        }
        if ((repCode != 200) && ((repCode < 300) || (repCode > 307))) {
            throw new IOException("Login failed");
        }

        String sessionid = null;
        headers = con.getHeaderFields();
        if (headers.containsKey("Set-Cookie")) {
            for (String item : headers.get("Set-Cookie")) {
                if (item.startsWith("sessionid=")) {
                    sessionid = item.split("=", 2)[1].split(";")[0];
                }
            }
        }
        if (sessionid == null) {
            throw new IOException("Login failed, sessionid not found");
        }

        // Login succeeded - store the session id!
        cookies.put("sessionid", sessionid);
    }

    private String obtainAppKey() throws IOException {
        HttpURLConnection con = doQuery("user/settings/");
        if (con.getResponseCode() != 200) {
            throw new IOException("Cannot access user settings");
        }

        String html = loadHtml(con);
        String flatHtml = html.replace("\n", "").replace("\r", "");

        Pattern pat = Pattern.compile("<div[^>]*>(.*)<input.*name=.api_key_regenerate");
        Matcher match = pat.matcher(flatHtml);
        if (!match.find()) {
            throw new IOException("Cannot find api-key section");
        }

        String keySection = match.group(1);
        int divIndex = keySection.lastIndexOf("<div");
        if (divIndex >= 0) {
            keySection = keySection.substring(divIndex);
        }
        String splits[] = keySection.replaceAll("<[^>]+>", " ").trim().split(" ");
        return splits[splits.length - 1];
    }

    private String loadHtml(HttpURLConnection con) throws IOException {
        // Load the html - max 1 MB
        byte[] buffer = new byte[1000000];

        int offset = 0;
        while (true) {
            if (buffer.length - offset <= 0) {
                break;
            }
            int len = con.getInputStream().read(buffer, offset, buffer.length - offset);
            if (len <= 0) {
                break;
            }
            offset += len;
        }

        String html;
        if (offset <= 0) {
            html = "";
        } else {
            html = new String(buffer, 0, offset, StandardCharsets.UTF_8);
        }
        return html;
    }
}