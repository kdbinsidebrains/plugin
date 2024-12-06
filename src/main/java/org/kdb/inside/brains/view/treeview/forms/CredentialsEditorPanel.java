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
    private CredentialEditor activeEditor;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private final InheritedCredentialProvider inherited;
    private final ComboBox<Object> editorsBox = new ComboBox<>();
    private final Map<String, CredentialEditor> editors = new HashMap<>();

    private List<ValidationInfo> myInfo = new ArrayList<>();
    private final List<CredentialProvider> providers = new ArrayList<>();


    private final TheCredentialChangeListener changeListener = new TheCredentialChangeListener();

    /**
     * Creates new global or a scope editor.
     * <p>
     * Depends on the flag, an inherited editor will be added (for a scope) or removed (for global settings) from the editor.
     */
    public CredentialsEditorPanel(boolean scopeEditor) {
        inherited = scopeEditor ? new InheritedCredentialProvider(null) : null;
        initPanel();
    }

    /**
     * Creates new an instance editor panel with specified parent scope.
     *
     * @param scope the parent scope for inherited settings
     */
    public CredentialsEditorPanel(KdbScope scope) {
        inherited = new InheritedCredentialProvider(scope);
        initPanel();
    }

    private void initPanel() {
        editorsBox.setEditable(false);
        editorsBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final String name = (String) e.getItem();
                cardLayout.show(cardPanel, name);

                activeEditor = editors.get(name);
                processCredentialChanged(activeEditor.getCredentials());
            }
        });

        updateCredentialProvider(CredentialService.getInstance().getProviders());

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
        return activeEditor == null ? null : activeEditor.getCredentials();
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
        final List<ValidationInfo> info = errors == null || errors.isEmpty() ? List.of() : errors.stream().map(e -> new ValidationInfo(e.message(), e.component())).collect(Collectors.toList());
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

    public void updateCredentialProvider(List<CredentialProvider> prov) {
        if (providers.equals(prov)) {
            return;
        }

        // remember
        final String credentials = getCredentials();

        editors.clear();
        providers.clear();
        cardPanel.removeAll();
        activeEditor = null;

        if (inherited != null) {
            providers.add(inherited);
        }
        providers.addAll(prov);

        int i = 0;
        final String[] names = new String[providers.size()];
        for (CredentialProvider provider : providers) {
            names[i] = provider.getName();

            final CredentialEditor editor = provider.createEditor();
            if (activeEditor == null) {
                activeEditor = editor;
            }
            editor.addCredentialChangeListener(changeListener);

            final JPanel p = new JPanel(new BorderLayout());
            p.add(editor, BorderLayout.NORTH);
            editors.put(names[i], editor);
            cardPanel.add(p, names[i]);
            i++;
        }

        editorsBox.setModel(new DefaultComboBoxModel<>(names));

        // and restore after
        setCredentials(credentials);
    }

    private class TheCredentialChangeListener implements CredentialChangeListener {
        @Override
        public void credentialsChanged(String credentials) {
            clearErrors();
            processCredentialChanged(credentials);
        }
    }
}