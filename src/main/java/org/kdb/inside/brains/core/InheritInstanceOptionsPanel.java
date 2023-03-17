package org.kdb.inside.brains.core;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.kdb.inside.brains.settings.KdbSettingsService;

import javax.swing.*;
import java.awt.*;

public class InheritInstanceOptionsPanel extends AbstractOptionsPanel {
    private InstanceOptions editableOptions;

    private final JBCheckBox inheritedOptions;

    private final InstanceOptions defaultOptions;
    private final InstanceOptionsPanel optionsPanel;

    public InheritInstanceOptionsPanel(InstanceOptions options) {
        this(null, options);
    }

    public InheritInstanceOptionsPanel(KdbScope scope, InstanceOptions options) {
        super(new BorderLayout());

        InstanceOptions parent = null;
        if (scope != null) {
            parent = scope.getOptions();
        }
        if (parent == null) {
            parent = KdbSettingsService.getInstance().getInstanceOptions();
        }
        defaultOptions = parent;
        editableOptions = options == null ? defaultOptions : options;

        this.optionsPanel = new InstanceOptionsPanel();
        this.optionsPanel.setInstanceOptions(editableOptions);
        this.optionsPanel.addOptionsChangedListener(o -> notifyOptionsChanged());

        this.inheritedOptions = new JBCheckBox("Inherit options from " + (scope != null ? " scope " + scope.getName() : " global configuration"));
        this.inheritedOptions.addActionListener(e -> changeOptions());
        this.inheritedOptions.setSelected(options == null);
        changeOptions();

        initPanel();
    }

    private void initPanel() {
        final FormBuilder builder = FormBuilder.createFormBuilder();
        final JPanel panel = builder
                .addComponent(inheritedOptions)
                .setFormLeftIndent(10)
                .addComponent(optionsPanel)
                .getPanel();
        add(panel, BorderLayout.CENTER);
    }

    private void changeOptions() {
        if (inheritedOptions.isSelected()) {
            editableOptions = optionsPanel.getInstanceOptions();

            optionsPanel.setEnabled(false);
            optionsPanel.setInstanceOptions(defaultOptions);
        } else {
            optionsPanel.setEnabled(true);
            optionsPanel.setInstanceOptions(editableOptions);
        }
        notifyOptionsChanged();
    }

    @Override
    public InstanceOptions getInstanceOptions() {
        return inheritedOptions.isSelected() ? null : optionsPanel.getInstanceOptions();
    }
}
