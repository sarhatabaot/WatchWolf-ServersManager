package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;
import dev.watchwolf.serversmanager.server.instantiator.ItzgDockerServerInstantiator;

/** Process-wide runtime selection. */
public final class MinecraftRuntimeProviders {
    private MinecraftRuntimeProviders() {}

    public static MinecraftRuntimeProvider fromEnvironment() {
        return select(System.getenv("WATCHWOLF_MINECRAFT_RUNTIME"));
    }

    public static MinecraftRuntimeProvider select(String configured) {
        if ("legacy".equals(configured)) {
            return new LegacyRuntimeProvider(new DockerizedServerInstantiator());
        }
        if (configured == null || configured.isBlank() || configured.equals("itzg")) {
            return new ItzgRuntimeProvider(new ItzgDockerServerInstantiator(),
                    configuredOrDefault("WATCHWOLF_ITZG_IMAGE", "itzg/minecraft-server"),
                    configuredOrDefault("WATCHWOLF_ITZG_TAG", "auto"));
        }
        throw new IllegalArgumentException("Unsupported WATCHWOLF_MINECRAFT_RUNTIME: " + configured
                + "; supported values are legacy and itzg");
    }

    private static String configuredOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
