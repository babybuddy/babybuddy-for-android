package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import eu.pkgsoftware.babybuddywidgets.networking.ServerAccessProviderInterface;

public class CredStore extends CredStoreEncryptionEngine implements ServerAccessProviderInterface {
    public static final Notes EMPTY_NOTES = new Notes("", false);

    public static class Notes {
        public String note;
        public boolean visible;

        public Notes(String note, boolean visible) {
            this.note = note;
            this.visible = visible;
        }

        public @NotNull Notes clone() {
            return new Notes(note, visible);
        }
    }

    public interface SettingsFileOpener {
        public InputStream openReadStream() throws IOException;

        public OutputStream openWriteStream() throws IOException;
    }

    public static class StaticPathOpener implements SettingsFileOpener {
        private final String path;

        public StaticPathOpener(String path) {
            this.path = path;
        }

        public InputStream openReadStream() throws IOException {
            return new FileInputStream(path);
        }

        public OutputStream openWriteStream() throws IOException {
            return new FileOutputStream(path);
        }
    }

    public final String CURRENT_VERSION;

    private @NotNull SettingsFileOpener settingsFileOpener;

    private String serverUrl;
    private String encryptedToken;
    private String encryptedCookies = null;
    private Map<String, Notes> notesAssignments = new HashMap<String, Notes>();
    private Double lastUsedAmount = null;
    private Map<String, Integer> tutorialParameters = new HashMap<>();
    private String storedVersion = "";

    private String currentChild = null;
    // private Map<String, String> children = new HashMap<>();

    public static String getAppVersionString(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        return pi.versionName;
    }

    private void loadCredsFile() {
        try {
            Properties props = new Properties();
            try (InputStream fis = settingsFileOpener.openReadStream()) {
                props.load(fis);
            } catch (IOException e) {
                // pass, but save current version
                props.put("stored_version", CURRENT_VERSION);
            }
            serverUrl = props.getProperty("server");
            String loadedSalt = props.getProperty("salt");
            if (loadedSalt != null) {
                setSALT(loadedSalt);
            }

            encryptedToken = props.getProperty("token");
            encryptedCookies = props.getProperty("cookies", null);

            storedVersion = props.getProperty("stored_version", "-1");

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

    public CredStore(Context context) {
        CURRENT_VERSION = getAppVersionString(context);
        settingsFileOpener = new StaticPathOpener(
            context.getFilesDir().getAbsolutePath() + "/settings.conf"
        );
        loadCredsFile();
    }

    public CredStore(@NotNull SettingsFileOpener opener, @NotNull String currentVersion) {
        CURRENT_VERSION = currentVersion;
        settingsFileOpener = opener;
        loadCredsFile();
    }

    private String stringMapToString(Map<String, String> m) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            String k = new String(
                Base64.encode(e.getKey().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP),
                StandardCharsets.UTF_8
            );
            String v = new String(
                Base64.encode(e.getValue().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP),
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
                Base64.decode(assignmentParts[0].trim(), Base64.NO_WRAP), StandardCharsets.UTF_8
            );
            String value = new String(
                Base64.decode(assignmentParts[1].trim(), Base64.NO_WRAP), StandardCharsets.UTF_8
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
        props.setProperty("salt", getSALT());
        if (encryptedToken != null) {
            props.setProperty("token", encryptedToken);
        }
        if (encryptedCookies != null) {
            props.setProperty("cookies", encryptedCookies);
        }

        props.put("stored_version", storedVersion);

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

        try (OutputStream fos = settingsFileOpener.openWriteStream()) {
            props.store(fos, "");
        } catch (IOException e) {
            // pass
        }
    }

    public @NotNull String getAppToken() {
        String result = decryptMessage(encryptedToken);
        if (result == null) {
            result = "";
        }
        return result;
    }

    public void storeAppToken(String token) {
        encryptedToken = encryptMessage(token);
        storePrefs();
    }

    public void storeServerUrl(String url) {
        serverUrl = url;
        storePrefs();
    }

    public @NotNull String getServerUrl() {
        if (serverUrl == null) {
            return "";
        }
        return serverUrl;
    }

    public String getSelectedChild() {
        return currentChild;
    }

    public void setSelectedChild(String c) {
        currentChild = c;
        storePrefs();
    }

    public void setObjectNotes(String id, boolean visible, String notes) {
        notesAssignments.put(id, new Notes(notes, visible));
    }

    public @NonNull Notes getObjectNotes(String id) {
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

    public void updateStoredVersion() {
        storedVersion = CURRENT_VERSION;
        storePrefs();
    }

    public boolean isStoredVersionOutdated() {
        return !CURRENT_VERSION.equals(storedVersion);
    }

    public @NotNull Map<String, String> getAuthCookies() {
        String encodedMap = decryptMessage(encryptedCookies);
        if (encodedMap == null) {
            return new HashMap<>();
        }
        return stringToStringMap(encodedMap);
    }

    public void storeAuthCookies(Map<String, String> cookies) {
        if ((cookies == null) || (cookies.size() == 0)) {
            encryptedCookies = null;
        } else {
            encryptedCookies = encryptMessage(stringMapToString(cookies));
        }
        storePrefs();
    }

    public void clearLoginData() {
        storeAppToken(null);
        storeAuthCookies(null);
        clearNotes();
    }
}