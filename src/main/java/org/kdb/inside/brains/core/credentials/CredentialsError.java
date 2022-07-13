package org.kdb.inside.brains.core.credentials;

import javax.swing.*;

public final class CredentialsError {
    private final String message;
    private final JComponent component;

    public CredentialsError(String message, JComponent component) {
        this.message = message;
        this.component = component;
    }

    public String getMessage() {
        return message;
    }

    public JComponent getComponent() {
        return component;
    }
}
