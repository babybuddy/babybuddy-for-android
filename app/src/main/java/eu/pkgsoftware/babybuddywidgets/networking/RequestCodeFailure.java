package eu.pkgsoftware.babybuddywidgets.networking;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This special exception provides functions that interface with django errors. In particular,
 * it contains methods that allow access to embedded server-reported error messages.
 */
public class RequestCodeFailure extends IOException {
    public @NotNull String response;
    public String content;

    private boolean decodedError = false;
    private JSONObject jsonErrorObject = null;

    public RequestCodeFailure(@NotNull String response, String content) {
        this.response = response;
        this.content = content;
    }

    private JSONObject getJSONErrorObject() {
        if (decodedError) {
            return jsonErrorObject;
        }

        decodedError = true;
        jsonErrorObject = null;
        if (content != null) {
            try {
                jsonErrorObject = new JSONObject(content);
            } catch (JSONException ignored) {
            }
        }
        return jsonErrorObject;
    }

    public boolean hasJSONMessage() {
        return getJSONErrorObject() != null;
    }

    public String[] jsonErrorMessages() {
        final JSONObject o = getJSONErrorObject();
        if (o == null) {
            return new String[0];
        }

        final List<String> messages = new ArrayList<>();
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            final JSONArray subArray = o.optJSONArray(it.next());
            if (subArray == null) {
                continue;
            }
            for (int i = 0; i < subArray.length(); ) {
                String msg = subArray.optString(i);
                if (msg != null) {
                    messages.add(msg);
                }
            }
        }
        return messages.toArray(new String[0]);
    }
}
