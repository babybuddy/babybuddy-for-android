package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Timer;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class CredStore {
    public static class Notes {
        public String note;
        public boolean visible;

        public Notes(String note, boolean visible) {
            this.note = note;
            this.visible = visible;
        }

        public Notes clone() {
            return new Notes(note, visible);
        }
    }

    public static final String ENCRYPTION_STRING = "gK,8kwXJZRmL6/yz&Dp;tr5&Muk,A;h,VGeb$qN-Gid3xLW&a/Xi0YOomVpQVAiFn:hP$8dbIX;L*v*cie&Tnkf+obFEN;a+DTmrILQO6CkY.oOV25dBjpXbep%qAu1bnbeS3A-zn%m";

    private String settingsFilePath;

    private String serverUrl;
    private String SALT_STRING;
    private String encryptedToken;
    private Map<Integer, Integer> timerAssignments = new HashMap<Integer, Integer>();
    private Map<String, Notes> notesAssignments = new HashMap<String, Notes>();
    private Double lastUsedAmount = null;
    private Map<String, Integer> tutorialParameters = new HashMap<>();

    private String currentChild = null;
    // private Map<String, String> children = new HashMap<>();

    public CredStore(Context context) {
        settingsFilePath = context.getFilesDir().getAbsolutePath().toString() + "/settings.conf";

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(settingsFilePath.toString())) {
                props.load(fis);
            } catch (IOException e) {
                // pass
            }
            serverUrl = props.getProperty("server");
            SALT_STRING = props.getProperty("salt");
            if (SALT_STRING == null) {
                generateNewSalt();
            }

            String timerAssignmentsString = props.getProperty("timer_default_types");
            if (timerAssignmentsString != null) {
                for (String ass : timerAssignmentsString.split(";")) {
                    if (ass.length() <= 0) continue;
                    String[] assParts = ass.split("=");
                    if (assParts.length != 2) continue;
                    timerAssignments.put(Integer.parseInt(assParts[0]), Integer.parseInt(assParts[1]));
                }
            }

            encryptedToken = props.getProperty("token");

            // children = props.getProperty("children_cache", "");
            currentChild = props.getProperty("selected_child", null);

            Enumeration<?> nameEnum = props.propertyNames();
            for (Object o : Collections.list(nameEnum)) {
                final String name = Objects.toString(o);
                if (name.startsWith("notes_")) {
                    final String noteName = name.replaceFirst(Pattern.quote("notes_"), "");
                    String n = props.getProperty(name);
                    if (!n.contains(":")) {
                        continue;
                    }
                    String[] splits = n.split(":", 2);
                    notesAssignments.put(noteName, new Notes(splits[1], "T".equals(splits[0])));
                }
            }

            lastUsedAmount = null;
            if (props.containsKey("last_used_amount")) {
                String s = props.getProperty("last_used_amount", "null");
                if ("null".equalsIgnoreCase(s)) {
                    lastUsedAmount = null;
                } else {
                    lastUsedAmount = Double.valueOf(s);
                }
            }

            if (props.containsKey("tutorial_parameters")) {
                Map<String, String> rawMap = stringToStringMap(props.getProperty("tutorial_parameters"));
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    try {
                        tutorialParameters.put(entry.getKey(), Integer.parseInt(entry.getValue()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private String stringMapToString(Map<String, String> m) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            String k = new String(
                Base64.encode(e.getKey().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT),
                StandardCharsets.UTF_8
            );
            String v = new String(
                Base64.encode(e.getValue().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT),
                StandardCharsets.UTF_8
            );
            result.append(k.trim()).append(":").append(v.trim()).append("\n");
        }
        return result.toString();
    }

    private Map<String, String> stringToStringMap(String s) {
        Map<String, String> result = new HashMap<>();
        for (String split : s.split("\n")) {
            String[] assignmentParts = split.trim().split(":");
            if (assignmentParts.length != 2) {
                continue;
            }

            String key = new String(
                Base64.decode(assignmentParts[0].trim(), Base64.DEFAULT), StandardCharsets.UTF_8
            );
            String value = new String(
                Base64.decode(assignmentParts[1].trim(), Base64.DEFAULT), StandardCharsets.UTF_8
            );
            result.put(key, value);
        }
        return result;
    }

    public void storePrefs() {
        Properties props = new Properties();
        if (serverUrl != null) {
            props.setProperty("server", serverUrl);
        }
        props.setProperty("salt", SALT_STRING);
        if (encryptedToken != null) {
            props.setProperty("token", encryptedToken);
        }

        StringBuilder timerAssignmentsString = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : timerAssignments.entrySet()) {
            timerAssignmentsString.append(e.getKey());
            timerAssignmentsString.append("=");
            timerAssignmentsString.append(e.getValue());
            timerAssignmentsString.append(";");
        }
        props.setProperty("timer_default_types", timerAssignmentsString.toString());

        // props.setProperty("children_cache", stringMapToString(children));
        if (currentChild != null) {
            props.setProperty("selected_child", currentChild);
        }

        for (Map.Entry<String, Notes> e : notesAssignments.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            props.setProperty("notes_" + e.getKey(), (e.getValue().visible ? "T" : "F") + ":" + e.getValue().note);
        }

        props.setProperty("last_used_amount", "" + lastUsedAmount);

        {
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : tutorialParameters.entrySet()) {
                stringMap.put(entry.getKey(), Integer.toString(entry.getValue()));
            }
            props.setProperty("tutorial_parameters", stringMapToString(stringMap));
        }

        try (FileOutputStream fos = new FileOutputStream(settingsFilePath.toString())) {
            props.store(fos, "");
        } catch (IOException e) {
            // pass
        }
    }

    private void generateNewSalt() {
        byte[] rnd = new byte[32];
        new SecureRandom().nextBytes(rnd);
        SALT_STRING = Base64.encodeToString(rnd, Base64.DEFAULT);
    }

    public String getAppToken() {
        try {
            if (encryptedToken == null) {
                return null;
            } else {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] rawKey = (SALT_STRING + ":::::" + ENCRYPTION_STRING).getBytes(StandardCharsets.ISO_8859_1);
                byte[] md5Key = MessageDigest.getInstance("MD5").digest(rawKey);
                byte[] ivGen = MessageDigest.getInstance("MD5").digest(
                    (new String(md5Key, StandardCharsets.ISO_8859_1) + ":::::" + SALT_STRING).getBytes(StandardCharsets.UTF_8));
                SecretKey key = new SecretKeySpec(md5Key, "AES");
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivGen));
                byte[] decoded = cipher.doFinal(Base64.decode(encryptedToken, Base64.DEFAULT));
                return new String(decoded, StandardCharsets.ISO_8859_1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void storeAppToken(String token) {
        try {
            if (token == null) {
                encryptedToken = null;
            } else {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                byte[] rawKey = (SALT_STRING + ":::::" + ENCRYPTION_STRING).getBytes(StandardCharsets.ISO_8859_1);
                byte[] md5Key = MessageDigest.getInstance("MD5").digest(rawKey);
                byte[] ivGen = MessageDigest.getInstance("MD5").digest(
                    (new String(md5Key, StandardCharsets.ISO_8859_1) + ":::::" + SALT_STRING).getBytes(StandardCharsets.UTF_8));
                SecretKey key = new SecretKeySpec(md5Key, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivGen));
                byte[] encoded = cipher.doFinal(token.getBytes(StandardCharsets.ISO_8859_1));
                encryptedToken = Base64.encodeToString(encoded, Base64.DEFAULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        storePrefs();
    }

    public void storeServerUrl(String url) {
        serverUrl = url;
        storePrefs();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setTimerDefaultSelection(int timer, int selection) {
        timerAssignments.put(timer, selection);
        storePrefs();
    }

    public Map<Integer, Integer> getTimerDefaultSelections() {
        return new HashMap<>(timerAssignments);
    }

    public String getSelectedChild() {
        return currentChild;
    }

    public void setSelectedChild(String c) {
        currentChild = c;
        storePrefs();
    }

    public void clearTimerAssociations() {
        timerAssignments.clear();
        storePrefs();
    }

    public void setObjectNotes(String id, boolean visible, String notes) {
        notesAssignments.put(id, new Notes(notes, visible));
    }

    private static Notes EMPTY_NOTES = new Notes("", false);

    public @NonNull
    Notes getObjectNotes(String id) {
        return notesAssignments.getOrDefault(id, EMPTY_NOTES).clone();
    }

    public void clearNotes() {
        notesAssignments.clear();
        storePrefs();
    }

    public void storeLastUsedAmount(Double amount) {
        lastUsedAmount = amount;
        storePrefs();
    }

    public Double getLastUsedAmount() {
        return lastUsedAmount;
    }

    public int getTutorialParameter(String s) {
        return tutorialParameters.getOrDefault(s, 0);
    }

    public void setTutorialParameter(String s, Integer i) {
        tutorialParameters.put(s, i);
        storePrefs();
    }
}