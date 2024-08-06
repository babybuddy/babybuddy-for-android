package eu.pkgsoftware.babybuddywidgets.networking;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ServerAccessProviderInterface {
    @NotNull String getAppToken();
    @NotNull String getServerUrl();
    @NotNull Map<String, String> getAuthCookies();
}
