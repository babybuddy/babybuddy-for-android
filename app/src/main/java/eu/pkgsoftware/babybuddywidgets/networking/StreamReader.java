package eu.pkgsoftware.babybuddywidgets.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;

public class StreamReader {
    @NonNull
    public static String loadHttpData(HttpURLConnection con) throws IOException {
        InputStream in = con.getInputStream();

        // Load the html - max 1 MB
        return loadHttpData(in);
    }

    @NonNull
    public static String loadHttpData(InputStream in) throws IOException {
        byte[] buffer = new byte[1000000];
        int offset = 0;
        int len;
        do {
            len = in.read(buffer, offset, buffer.length - offset);
            if (len > 0) {
                offset += len;
            }
        } while (len > 0 && offset < buffer.length);

        String html;
        if (offset <= 0) {
            html = "";
        } else {
            html = new String(buffer, 0, offset, StandardCharsets.UTF_8);
        }
        return html;
    }
}
