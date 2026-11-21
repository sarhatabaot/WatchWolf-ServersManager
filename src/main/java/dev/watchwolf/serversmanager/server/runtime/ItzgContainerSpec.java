package dev.watchwolf.serversmanager.server.runtime;

import java.util.Map;
import java.util.regex.Pattern;

/** Docker-independent part of the first supported itzg request. */
public record ItzgContainerSpec(String image, String serverVersion, Map<String, String> environment,
                                Map<String, String> labels) {
    private static final Pattern RELEASE_VERSION = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");

    public static ItzgContainerSpec paper(String serverType, String serverVersion, String image, String tag,
                                          String instanceId) {
        if (!"Paper".equals(serverType)) {
            throw new IllegalArgumentException("itzg currently supports only Paper; requested " + serverType);
        }
        if (serverVersion == null || !RELEASE_VERSION.matcher(serverVersion).matches()) {
            throw new IllegalArgumentException("Paper version must use a numeric release format such as 1.21.1; requested "
                    + serverVersion);
        }
        if (image == null || image.isBlank() || tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("WATCHWOLF_ITZG_IMAGE and WATCHWOLF_ITZG_TAG must be nonempty");
        }
        String selectedTag = "auto".equals(tag)
                ? ("1.20.6".equals(serverVersion) ? "java21" : "java17") : tag;
        return new ItzgContainerSpec(image + ":" + selectedTag, serverVersion,
                Map.of("EULA", "TRUE", "TYPE", "PAPER", "VERSION", serverVersion,
                        "ENABLE_RCON", "FALSE", "UID", "1000", "GID", "1000",
                        "SKIP_DOWNLOAD_DEFAULTS", "TRUE"),
                Map.of("watchwolf.managed", "true", "watchwolf.component", "minecraft-server",
                        "watchwolf.runtime", "itzg", "watchwolf.instance-id", instanceId,
                        "watchwolf.server-type", "paper", "watchwolf.server-version", serverVersion));
    }
}
