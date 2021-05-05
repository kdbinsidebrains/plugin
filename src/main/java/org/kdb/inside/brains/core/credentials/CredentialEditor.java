package org.kdb.inside.brains.core.credentials;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class CredentialEditor extends JPanel {
    protected final List<CredentialChangeListener> credentialListeners = new CopyOnWriteArrayList<>();

    public void addCredentialChangeListener(CredentialChangeListener l) {
        if (l != null) {
            credentialListeners.add(l);
        }
    }

    public void removeCredentialChangeListener(CredentialChangeListener l) {
        if (l != null) {
            credentialListeners.remove(l);
        }
    }

    public abstract String getCredentials();

    public abstract String getViewableCredentials();


    public abstract void setCredentials(String credentials);


    public abstract List<CredentialsError> validateEditor();


    protected void processCredentialChanged(String credentials) {
        credentialListeners.forEach(l -> l.credentialsChanged(credentials));
    }
}
