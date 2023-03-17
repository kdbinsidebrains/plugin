package org.kdb.inside.brains.view.treeview.scope;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.core.credentials.CredentialsError;
import org.kdb.inside.brains.view.treeview.forms.CredentialsEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class ScopeConfigurable extends NamedConfigurable<KdbScope> {
    private KdbScope originalScope;
    private KdbScope editableScope;

    private final KdbScopesManager scopesManager;
    private final InstanceOptionsPanel optionsPanel;
    private final CredentialsEditorPanel credentialsPanel;

    private final JCheckBox sharedCheckbox;
    private final JBLabel sharedContextHelpLabel;

    public ScopeConfigurable(final KdbScope scope, KdbScopesManager scopesManager, final Runnable updateTree) {
        super(true, updateTree);

        this.originalScope = scope;
        this.editableScope = scope;
        this.scopesManager = scopesManager;

        optionsPanel = new InstanceOptionsPanel(true);
        optionsPanel.setInstanceOptions(scope.getOptions());

        credentialsPanel = new CredentialsEditorPanel(true);
        credentialsPanel.setCredentials(scope.getCredentials());
        credentialsPanel.setBorder(JBUI.Borders.empty(0, 10, 10, 10));

        sharedCheckbox = new JCheckBox("Shared Kdb Scope", scope.getType() == ScopeType.SHARED);

        sharedContextHelpLabel = new JBLabel(AllIcons.General.ContextHelp);
        sharedContextHelpLabel.setToolTipText("Shared KDB context is shared across the application and available for all projects");
        sharedContextHelpLabel.setBorder(JBUI.Borders.empty(0, 5));
    }

    @Override
    public void setDisplayName(final String name) {
        if (Comparing.strEqual(editableScope.getName(), name)) {
            return;
        }
        editableScope = new KdbScope(name, getCurrentType(), getInstanceOptions(), getCurrentCredentials());
    }

    @Override
    public String getDisplayName() {
        return editableScope.getName();
    }

    @Override
    public String getBannerSlogan() {
        return "Scope " + editableScope.getName();
    }

    public KdbScope getOriginalScope() {
        return originalScope;
    }

    @Override
    public KdbScope getEditableObject() {
        return new KdbScope(editableScope.getName(), getCurrentType(), getInstanceOptions(), getCurrentCredentials());
    }

    @Override
    protected JComponent createTopRightComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(BorderLayout.WEST, sharedCheckbox);
        panel.add(BorderLayout.EAST, sharedContextHelpLabel);
        return panel;
    }

    @Override
    public JComponent createOptionsPanel() {
        final FormBuilder formBuilder = FormBuilder.createFormBuilder();

        formBuilder
                .setFormLeftIndent(10)
                .addComponent(new TitledSeparator("Scope Authentication"))
                .setFormLeftIndent(20)
                .addComponent(credentialsPanel)

                .setFormLeftIndent(10)
                .addComponent(new TitledSeparator("Scope Connection Options"))
                .setFormLeftIndent(30)
                .addComponent(optionsPanel)

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    @Override
    public boolean isModified() {
        if (originalScope.getType() != getCurrentType()) {
            return true;
        }
        if (!Objects.equals(originalScope.getName(), editableScope.getName())) {
            return true;
        }
        if (!Objects.equals(originalScope.getCredentials(), getCurrentCredentials())) {
            return true;
        }
        if (!Comparing.equal(originalScope.getOptions(), getInstanceOptions())) {
            return true;
        }
        return !scopesManager.containsScope(originalScope);
    }

    @Override
    public void apply() throws ConfigurationException {
        final List<CredentialsError> credentialsErrors = credentialsPanel.validateEditor();
        if (credentialsErrors != null && !credentialsErrors.isEmpty()) {
            throw new ConfigurationException("Some credentials parameters are wrong. Please check appropriate fields.");
        }

        editableScope = new KdbScope(editableScope.getName(), getCurrentType(), getInstanceOptions(), getCurrentCredentials());
        if (scopesManager.containsScope(originalScope)) {
            originalScope.update(editableScope);
        } else {
            // It's new scope, let's copy all items (imported scope?)
            originalScope.forEach(editableScope::copyItem);

            scopesManager.addScope(editableScope);
            originalScope = editableScope;
        }
    }

    @Override
    public void reset() {
        editableScope = originalScope;
        sharedCheckbox.setSelected(originalScope.getType() == ScopeType.SHARED);
        credentialsPanel.setCredentials(originalScope.getCredentials());
    }

    @Nullable
    @Override
    public Icon getIcon(boolean expanded) {
        return getCurrentType().getIcon();
    }

    private ScopeType getCurrentType() {
        return sharedCheckbox.isSelected() ? ScopeType.SHARED : ScopeType.LOCAL;
    }

    private String getCurrentCredentials() {
        return credentialsPanel.getCredentials();
    }

    private InstanceOptions getInstanceOptions() {
        return optionsPanel.getInstanceOptions();
    }
}
