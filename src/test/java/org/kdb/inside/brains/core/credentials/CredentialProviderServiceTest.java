package org.kdb.inside.brains.core.credentials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredentialProviderServiceTest {
    @Test
    void resolve() {
        assertEquals("qweqw", CredentialProviderService.resolveProperties("qweqw"));

        assertEquals("qwr" + System.getProperty("user.name") + "dasfasdf", CredentialProviderService.resolveProperties("qwr${user.name}dasfasdf"));

        assertEquals("qwr" + System.getProperty("user.name") + "dasf" + System.getProperty("user.name") + "asdf" + System.getProperty("java.version"), CredentialProviderService.resolveProperties("qwr${user.name}dasf${user.name}asdf${java.version}"));
    }
}