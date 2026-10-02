package dev.watchwolf.serversmanager.server.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItzgContainerSpecShould {
    @Test
    public void mapTheFirstSupportedPaperRequestToImageEnvironmentAndLabels() {
        ItzgContainerSpec spec = ItzgContainerSpec.paper("Paper", "1.20.6",
                "itzg/minecraft-server", "java21", "123");

        assertEquals("itzg/minecraft-server:java21", spec.image());
        assertEquals("TRUE", spec.environment().get("EULA"));
        assertEquals("PAPER", spec.environment().get("TYPE"));
        assertEquals("1.20.6", spec.environment().get("VERSION"));
        assertEquals("FALSE", spec.environment().get("ENABLE_RCON"));
        assertEquals("1000", spec.environment().get("UID"));
        assertEquals("1000", spec.environment().get("GID"));
        assertEquals("TRUE", spec.environment().get("SKIP_DOWNLOAD_DEFAULTS"));
        assertEquals("true", spec.labels().get("watchwolf.managed"));
        assertEquals("123", spec.labels().get("watchwolf.instance-id"));
    }

    @Test
    public void rejectUnverifiedTypesAndVersions() {
        assertThrows(IllegalArgumentException.class, () -> ItzgContainerSpec.paper(
                "Spigot", "1.20.6", "itzg/minecraft-server", "java21", "123"));
        assertThrows(IllegalArgumentException.class, () -> ItzgContainerSpec.paper(
                "Paper", "1.21", "itzg/minecraft-server", "java21", "123"));
    }

    @Test
    public void chooseTheJavaImageForEachSupportedPaperVersion() {
        assertEquals("itzg/minecraft-server:java17", ItzgContainerSpec.paper(
                "Paper", "1.19", "itzg/minecraft-server", "auto", "123").image());
        assertEquals("itzg/minecraft-server:java17", ItzgContainerSpec.paper(
                "Paper", "1.20.2", "itzg/minecraft-server", "auto", "123").image());
        assertEquals("itzg/minecraft-server:java21", ItzgContainerSpec.paper(
                "Paper", "1.20.6", "itzg/minecraft-server", "auto", "123").image());
    }
}
