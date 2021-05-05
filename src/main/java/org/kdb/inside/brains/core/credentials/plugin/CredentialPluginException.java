package org.kdb.inside.brains.core.credentials.plugin;

public class CredentialPluginException extends Exception {
    public CredentialPluginException(String message) {
        super(message);
    }

    public CredentialPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
