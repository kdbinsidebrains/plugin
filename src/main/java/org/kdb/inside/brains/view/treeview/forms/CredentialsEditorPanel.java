package org.kdb.inside.brains.view.treeview.forms;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.credentials.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CredentialsEditorPanel extends CredentialEditor implements Disposable {
    private ComboBox<Object> editorsBox;
    private CredentialEditor activeEditor;

    private List<ValidationInfo> myInfo = new ArrayList<>();
    private final List<CredentialProvider> providers = new ArrayList<>();

    private final TheCredentialChangeListener changeListener = new TheCredentialChangeListener();

    /**
     * Creates new global or a scope editor.
     * <p>
     * Depends on the flag an inherited editor will be added (for a scope) or removed (for global settings) from the editor.
     */
    public CredentialsEditorPanel(boolean scopeEditor) {
        initPanel(null, scopeEditor);
    }

    /**
     * Creates new an instance editor panel with specified parent scope.
     *
     * @param scope the parent scope for inherited settings
     */
    public CredentialsEditorPanel(KdbScope scope) {
        initPanel(scope, true);
    }

    private void initPanel(KdbScope scope, boolean inherit) {
        final CardLayout cardLayout = new CardLayout();
        final JPanel cardPanel = new JPanel(cardLayout);

        if (inherit) {
            providers.add(new InheritedCredentialProvider(scope));
        }
        providers.addAll(CredentialService.getInstance().getProviders());

        int i = 0;
        final String[] names = new String[providers.size()];
        final Map<String, CredentialEditor> editors = new HashMap<>(providers.size());
        for (CredentialProvider provider : providers) {
            names[i] = provider.getName();

            final CredentialEditor editor = provider.createEditor();
            if (activeEditor == null) {
                activeEditor = editor;
            }
            editor.addCredentialChangeListener(changeListener);

            final JPanel p = new JPanel(new BorderLayout());
            p.add(editor, BorderLayout.NORTH);
            cardPanel.add(p, names[i]);
            editors.put(names[i], editor);
            i++;
        }

        editorsBox = new ComboBox<>(names);
        editorsBox.setEditable(false);
        editorsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final String name = (String) e.getItem();
                cardLayout.show(cardPanel, name);

                activeEditor = editors.get(name);
                processCredentialChanged(activeEditor.getCredentials());
            }
        });

        final BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(layout);

        add(editorsBox);
        add(Box.createVerticalStrut(3));

        final JPanel p = new JPanel(new BorderLayout());
        p.add(cardPanel, BorderLayout.NORTH);

        add(p);
    }

    @Override
    public String getCredentials() {
        return activeEditor.getCredentials();
    }

    @Override
    public String getViewableCredentials() {
        return activeEditor.getViewableCredentials();
    }

    @Override
    public void setCredentials(String credentials) {
        final CredentialProvider provider = CredentialService.findProvider(providers, credentials);
        final String name = provider.getName();
        if (!name.equals(editorsBox.getSelectedItem())) {
            editorsBox.setSelectedItem(name);
        }
        activeEditor.setCredentials(credentials);
        clearErrors();
    }

    @Override
    public List<CredentialsError> validateEditor() {
        return updateErrors(activeEditor.validateEditor());
    }

    @Override
    public void dispose() {
        clearErrors();
    }

    private void clearErrors() {
        updateErrors(null);
    }

    private List<CredentialsError> updateErrors(List<CredentialsError> errors) {
        final List<ValidationInfo> info = errors == null || errors.isEmpty() ? List.of() : errors.stream().map(e -> new ValidationInfo(e.getMessage(), e.getComponent())).collect(Collectors.toList());
        if (!myInfo.equals(info)) {
            updateComponentErrors(info);
            myInfo = info;
        }
        return errors;
    }

    private void updateComponentErrors(List<ValidationInfo> info) {
        // clear current component errors
        myInfo.stream()
                .filter(vi -> !info.contains(vi))
                .filter(vi -> vi.component != null)
                .map(vi -> ComponentValidator.getInstance(vi.component))
                .forEach(c -> c.ifPresent(vi -> vi.updateInfo(null)));

        // show current errors
        for (ValidationInfo vi : info) {
            JComponent component = vi.component;
            if (component == null) {
                continue;
            }
            ComponentValidator.getInstance(component).orElseGet(() -> new ComponentValidator(this).installOn(component)).updateInfo(vi);
        }
    }

    private class TheCredentialChangeListener implements CredentialChangeListener {
        @Override
        public void credentialsChanged(String credentials) {
            clearErrors();
            processCredentialChanged(credentials);
        }
    }
}