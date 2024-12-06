package org.kdb.inside.brains.core.credentials;

import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.GridBag;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public final class UsernameCredentialProvider implements CredentialProvider {
    public static final UsernameCredentialProvider INSTANCE = new UsernameCredentialProvider();

    private UsernameCredentialProvider() {
    }

    @Override
    public String getName() {
        return "Username/Password Credentials";
    }

    @Override
    public boolean isSupported(String credentials) {
        return credentials != null;
    }

    @Override
    public CredentialEditor createEditor() {
        return new UsernameCredentialsEditor();
    }

    private static class UsernameCredentialsEditor extends CredentialEditor {
        final JBTextField usernameField = new JBTextField(CredentialService.DEFAULT_CREDENTIALS);
        final JBPasswordField passwordField = new JBPasswordField();

        public UsernameCredentialsEditor() {
            final GridBag c = new GridBag()
                    .setDefaultAnchor(0, GridBagConstraints.LINE_START)
                    .setDefaultAnchor(1, GridBagConstraints.CENTER)
                    .setDefaultWeightX(1, 1)
                    .setDefaultFill(GridBagConstraints.HORIZONTAL)
                    .setDefaultInsets(3, 10, 3, 3);

            setLayout(new GridBagLayout());

            add(new JLabel("Username:"), c.nextLine().next());
            add(usernameField, c.next());

            add(new JLabel("Password:"), c.nextLine().next());
            add(passwordField, c.next());

            final DocumentListener documentListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent documentEvent) {
                    processCredentialChanged(getCredentials());
                }

                @Override
                public void removeUpdate(DocumentEvent documentEvent) {
                    processCredentialChanged(getCredentials());
                }

                @Override
                public void changedUpdate(DocumentEvent documentEvent) {
                    processCredentialChanged(getCredentials());
                }
            };

            usernameField.getDocument().addDocumentListener(documentListener);
            passwordField.getDocument().addDocumentListener(documentListener);
        }

        @Override
        public String getCredentials() {
            if (passwordField.getPassword().length == 0) {
                return usernameField.getText();
            }
            return usernameField.getText() + ":" + new String(passwordField.getPassword());
        }

        @Override
        public String getViewableCredentials() {
            if (passwordField.getPassword().length == 0) {
                return usernameField.getText();
            }
            return usernameField.getText() + ":*****";
        }

        @Override
        public void setCredentials(String credentials) {
            usernameField.setText("");
            passwordField.setText("");

            if (credentials == null) {
                return;
            }

            final int i = credentials.indexOf(':');
            if (i >= 0) {
                usernameField.setText(credentials.substring(0, i));
                passwordField.setText(credentials.substring(i + 1));
            } else {
                usernameField.setText(credentials);
            }
        }

        @Override
        public List<CredentialsError> validateEditor() {
            return null;
        }
    }
}
