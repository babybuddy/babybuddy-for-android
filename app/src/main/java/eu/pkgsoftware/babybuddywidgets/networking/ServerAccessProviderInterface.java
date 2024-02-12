package eu.pkgsoftware.babybuddywidgets.networking;

import org.jetbrains.annotations.NotNull;

public interface ServerAccessProviderInterface {
    @NotNull String getAppToken();
    @NotNull String getServerUrl();
}
