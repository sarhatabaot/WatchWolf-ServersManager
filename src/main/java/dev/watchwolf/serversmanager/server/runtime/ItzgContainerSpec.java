package dev.watchwolf.serversmanager.server.runtime;

import java.util.Map;
import java.util.Set;

/** Docker-independent part of the first supported itzg request. */
public record ItzgContainerSpec(String image, String serverVersion, Map<String, String> environment,
                                Map<String, String> labels) {
    public static final Set<String> SUPPORTED_VERSIONS = Set.of("1.19", "1.20.2", "1.20.6");

    public static ItzgContainerSpec paper(String serverType, String serverVersion, String image, String tag,
                                          String instanceId) {
        if (!"Paper".equals(serverType) || !SUPPORTED_VERSIONS.contains(serverVersion)) {
            throw new IllegalArgumentException("itzg currently supports Paper " + SUPPORTED_VERSIONS
                    + "; requested " + serverType + " " + serverVersion);
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
