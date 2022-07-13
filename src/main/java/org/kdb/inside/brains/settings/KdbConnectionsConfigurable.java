package org.kdb.inside.brains.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceOptionsPanel;
import org.kdb.inside.brains.core.credentials.CredentialProviderService;
import org.kdb.inside.brains.core.credentials.CredentialsError;
import org.kdb.inside.brains.core.credentials.plugin.CredentialPluginsPanel;
import org.kdb.inside.brains.view.treeview.forms.CredentialsEditorPanel;

import javax.swing.*;
import java.util.List;

public class KdbConnectionsConfigurable extends KdbConfigurable {
    private final CredentialPluginsPanel pluginsPanel = new CredentialPluginsPanel();
    private final CredentialsEditorPanel credentialsPanel = new CredentialsEditorPanel(false);
    private final InstanceOptionsPanel instanceOptionsPanel = new InstanceOptionsPanel();

    private final KdbSettingsService settingsService = KdbSettingsService.getInstance();

    protected KdbConnectionsConfigurable() {
        super("Kdb.Settings.Connections", "Connections");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder
                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Authentication Plugins"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(pluginsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Default Authentication"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(credentialsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Connection Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(instanceOptionsPanel)

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    @Override
    public boolean isModified() {
        if (!Comparing.strEqual(settingsService.getDefaultCredentials(), credentialsPanel.getCredentials())) {
            return true;
        }
        if (!Comparing.equal(settingsService.getInstanceOptions(), instanceOptionsPanel.getInstanceOptions())) {
            return true;
        }
        return !CredentialProviderService.getInstance().getCredentialPlugins().equals(pluginsPanel.getCredentialPlugin());
    }

    @Override
    public void apply() throws ConfigurationException {
        final List<CredentialsError> credentialsErrors = credentialsPanel.validateEditor();
        if (credentialsErrors != null && !credentialsErrors.isEmpty()) {
            throw new ConfigurationException("Some credentials parameters are wrong. Please check appropriate fields.");
        }

        settingsService.setDefaultCredentials(credentialsPanel.getCredentials());
        settingsService.setInstanceOptions(instanceOptionsPanel.getInstanceOptions());
        CredentialProviderService.getInstance().setCredentialPlugin(pluginsPanel.getCredentialPlugin());
    }

    @Override
    public void reset() {
        pluginsPanel.setCredentialPlugins(CredentialProviderService.getInstance().getCredentialPlugins());
        instanceOptionsPanel.setInstanceOptions(settingsService.getInstanceOptions());
        credentialsPanel.setCredentials(settingsService.getDefaultCredentials());
    }
}