package org.kdb.inside.brains.core.credentials;

public interface CredentialProvider {
    String getName();

    default String getVersion() {
        return "undefined";
    }

    default String getDescription() {
        return null;
    }

    boolean isSupported(String credentials);

    CredentialEditor createEditor();


    String resolveCredentials(String credentials) throws CredentialsResolvingException;
}