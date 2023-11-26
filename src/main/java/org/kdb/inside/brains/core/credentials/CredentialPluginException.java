package org.kdb.inside.brains.core.credentials;

public class CredentialPluginException extends Exception {
    CredentialPluginException(String message) {
        super(message);
    }

    CredentialPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
