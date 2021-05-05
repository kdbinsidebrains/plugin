package org.kdb.inside.brains.core.credentials;

public class CredentialsResolvingException extends Exception {
    public CredentialsResolvingException(String message) {
        super(message);
    }

    public CredentialsResolvingException(String message, Throwable cause) {
        super(message, cause);
    }
}
