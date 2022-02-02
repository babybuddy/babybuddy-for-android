package eu.pkgsoftware.babybuddywidgets.networking;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class StreamReader {
    public static String loadHtml(HttpURLConnection con) throws IOException {
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
