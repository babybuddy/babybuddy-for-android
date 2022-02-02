package eu.pkgsoftware.babybuddywidgets.networking;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserFormInteractions {
    public static String extractCsrfmiddlewaretoken(String html) throws IOException {
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
        return csrfmiddlewaretoken;
    }

}
