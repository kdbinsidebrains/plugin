package org.kdb.inside.brains.core.credentials;

class CredentialPluginException extends Exception {
    CredentialPluginException(String message) {
        super(message);
    }

    CredentialPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
