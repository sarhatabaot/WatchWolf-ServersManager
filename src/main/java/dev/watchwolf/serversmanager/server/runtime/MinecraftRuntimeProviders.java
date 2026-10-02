package dev.watchwolf.serversmanager.server.runtime;

import dev.watchwolf.serversmanager.server.instantiator.DockerizedServerInstantiator;

/** Process-wide runtime selection. The itzg implementation is introduced in phase 2. */
public final class MinecraftRuntimeProviders {
    private MinecraftRuntimeProviders() {}

    public static MinecraftRuntimeProvider fromEnvironment() {
        return select(System.getenv("WATCHWOLF_MINECRAFT_RUNTIME"));
    }

    public static MinecraftRuntimeProvider select(String configured) {
        if (configured == null || configured.isBlank() || configured.equals("legacy")) {
            return new LegacyRuntimeProvider(new DockerizedServerInstantiator());
        }
        throw new IllegalArgumentException("Unsupported WATCHWOLF_MINECRAFT_RUNTIME: " + configured
                + "; phase 1 supports only legacy");
    }
}
