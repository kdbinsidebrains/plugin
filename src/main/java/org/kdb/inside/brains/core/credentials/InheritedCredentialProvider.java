package org.kdb.inside.brains.core.credentials;

import org.kdb.inside.brains.core.KdbScope;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Stream;

public final class InheritedCredentialProvider implements CredentialProvider {
    private final KdbScope scope;

    public InheritedCredentialProvider(KdbScope scope) {
        this.scope = scope;
    }

    @Override
    public String getName() {
        return "Inherited Credentials";
    }

    @Override
    public boolean isSupported(String credentials) {
        return credentials == null;
    }

    @Override
    public CredentialEditor createEditor() {
        final String name;
        final String credentials;
        if (scope == null || scope.getCredentials() == null) {
            name = "global settings";
            credentials = CredentialService.getInstance().getDefaultCredentials();
        } else {
            name = "scope " + scope.getName();
            credentials = scope.getCredentials();
        }

        final CredentialProvider provider = CredentialService.findProvider(credentials);

        final CredentialEditor editor = provider.createEditor();
        editor.setCredentials(credentials);

        return new TheCredentialsEditor(editor, name);
    }

    private static class TheCredentialsEditor extends CredentialEditor {
        private final CredentialEditor editor;

        private TheCredentialsEditor(CredentialEditor editor, String parentConfigName) {
            this.editor = editor;

            setLayout(new BorderLayout());
            add(new JLabel("The credentials are inherited from " + parentConfigName), BorderLayout.PAGE_START);
            add(editor, BorderLayout.CENTER);

            disableComponentDeep(editor);
        }

        private void disableComponentDeep(Container c) {
            c.setEnabled(false);
            Stream.of(c.getComponents()).filter(p -> p instanceof Container).map(p -> (Container) p).forEach(this::disableComponentDeep);
        }

        @Override
        public String getCredentials() {
            return null;
        }

        @Override
        public String getViewableCredentials() {
            return null;
        }

        @Override
        public void setCredentials(String credentials) {
            if (credentials != null) {
                throw new IllegalStateException("Update is not supported");
            }
        }

        @Override
        public List<CredentialsError> validateEditor() {
            return editor.validateEditor();
        }
    }
}