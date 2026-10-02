package dev.watchwolf.serversmanager.server;

/** A runtime rejected or failed to launch a requested Minecraft instance. */
public class ServerProvisioningException extends RuntimeException {
    public ServerProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
