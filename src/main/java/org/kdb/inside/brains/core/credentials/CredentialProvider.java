package org.kdb.inside.brains.core.credentials;

import java.util.Collection;

public interface CredentialProvider {
    String getName();

    boolean isSupported(String credentials);


    CredentialEditor createEditor();


    String resolveCredentials(String credentials) throws CredentialsResolvingException;


    static CredentialProvider findProvider(Collection<CredentialProvider> providers, String credentials) {
        return providers.stream().filter(p -> p.isSupported(credentials)).findFirst().orElse(UsernameCredentialProvider.INSTANCE);
    }
}