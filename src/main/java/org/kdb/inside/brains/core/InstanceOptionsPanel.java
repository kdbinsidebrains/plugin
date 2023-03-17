package org.kdb.inside.brains.core;

import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InstanceOptionsPanel extends JPanel {
    private final JBCheckBox tlsCheckbox = new JBCheckBox("TLS is enabled");
    private final JBCheckBox tlsCheckboxOverride = new JBCheckBox();

    private final JBCheckBox zipCheckbox = new JBCheckBox("Compression is enabled");
    private final JBCheckBox zipCheckboxOverride = new JBCheckBox();

    private final JBCheckBox asyncCheckbox = new JBCheckBox("Asynchronous connection");
    private final JBCheckBox asyncCheckboxOverride = new JBCheckBox();

    private final JBLabel timeoutLabel = new JBLabel("Connection timeout, ms: ");
    private final JBIntSpinner timeoutSpinner = new JBIntSpinner(1000, 1000, Integer.MAX_VALUE, 10);
    private final JBCheckBox timeoutSpinnerOverride = new JBCheckBox();

    private final boolean inherited;
    private final InstanceOptions parentOptions;

    private final List<InstanceOptionsListener> listeners = new CopyOnWriteArrayList<>();

    public InstanceOptionsPanel(boolean inherited) {
        this(inherited, null);
    }

    public InstanceOptionsPanel(KdbScope scope) {
        this(true, scope);
    }

    private InstanceOptionsPanel(boolean inherited, KdbScope scope) {
        super(new BorderLayout());

        this.inherited = inherited;
        this.parentOptions = InstanceOptions.resolveOptions(scope);

        final var form = FormBuilder.createFormBuilder();

        if (inherited) {
            form.addComponent(new JBLabel("Enable an option to override the inherited value"));
        }

        tlsCheckbox.addItemListener(e -> notifyOptionsChanged());
        tlsCheckboxOverride.addItemListener(e -> {
            tlsCheckbox.setEnabled(tlsCheckboxOverride.isSelected());
            notifyOptionsChanged();
        });

        zipCheckbox.addItemListener(e -> notifyOptionsChanged());
        zipCheckboxOverride.addItemListener(e -> {
            zipCheckbox.setEnabled(zipCheckboxOverride.isSelected());
            notifyOptionsChanged();
        });

        asyncCheckbox.addItemListener(e -> notifyOptionsChanged());
        asyncCheckboxOverride.addItemListener(e -> {
            asyncCheckbox.setEnabled(asyncCheckboxOverride.isSelected());
            notifyOptionsChanged();
        });

        timeoutSpinner.addChangeListener(e -> notifyOptionsChanged());
        timeoutSpinnerOverride.addChangeListener(e -> {
            timeoutLabel.setEnabled(timeoutSpinnerOverride.isSelected());
            timeoutSpinner.setEnabled(timeoutSpinnerOverride.isSelected());
            notifyOptionsChanged();
        });

        final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.add(timeoutLabel);
        p.add(timeoutSpinner);

        if (inherited) {
            form
                    .addLabeledComponent(tlsCheckboxOverride, tlsCheckbox)
                    .addLabeledComponent(zipCheckboxOverride, zipCheckbox)
                    .addLabeledComponent(asyncCheckboxOverride, asyncCheckbox)
                    .addLabeledComponent(timeoutSpinnerOverride, p);
        } else {
            form
                    .addComponent(tlsCheckbox)
                    .addComponent(zipCheckbox)
                    .addComponent(asyncCheckbox)
                    .addComponent(p);
        }

        form.addComponentFillVertically(new JPanel(), 0);

        add(form.getPanel());
    }

    public void addOptionsChangedListener(InstanceOptionsListener l) {
        if (listeners != null) {
            listeners.add(l);
        }
    }

    public void removeOptionsChangedListener(InstanceOptionsListener l) {
        if (listeners != null) {
            listeners.remove(l);
        }
    }

    public InstanceOptions getInstanceOptions() {
        final InstanceOptions.Builder b = new InstanceOptions.Builder();
        if (!inherited || tlsCheckboxOverride.isSelected()) {
            b.tls(tlsCheckbox.isSelected());
        }

        if (!inherited || zipCheckboxOverride.isSelected()) {
            b.zip(zipCheckbox.isSelected());
        }

        if (!inherited || asyncCheckboxOverride.isSelected()) {
            b.async(asyncCheckbox.isSelected());
        }

        if (!inherited || timeoutSpinnerOverride.isSelected()) {
            b.timeout(timeoutSpinner.getNumber());
        }
        return b.create();
    }

    public void setInstanceOptions(@Nullable InstanceOptions options) {
        final boolean hasTsl = options != null && options.hasTls();
        tlsCheckboxOverride.setSelected(hasTsl);
        tlsCheckbox.setEnabled(hasTsl);
        tlsCheckbox.setSelected(hasTsl ? options.isSafeTls() : parentOptions.isSafeTls());

        final boolean hasZip = options != null && options.hasZip();
        zipCheckboxOverride.setSelected(hasZip);
        zipCheckbox.setEnabled(hasZip);
        zipCheckbox.setSelected(hasZip ? options.isSafeZip() : parentOptions.isSafeZip());

        final boolean hasAsync = options != null && options.hasAsync();
        asyncCheckboxOverride.setSelected(hasAsync);
        asyncCheckbox.setEnabled(hasAsync);
        asyncCheckbox.setSelected(hasAsync ? options.isSafeAsync() : parentOptions.isSafeAsync());

        final boolean hasTimeout = options != null && options.hasTimeout();
        timeoutSpinnerOverride.setSelected(hasTimeout);
        timeoutLabel.setEnabled(hasTimeout);
        timeoutSpinner.setEnabled(hasTimeout);
        timeoutSpinner.setValue(hasTimeout ? options.getSafeTimeout() : parentOptions.getSafeTimeout());
    }

    protected void notifyOptionsChanged() {
        final InstanceOptions instanceOptions = getInstanceOptions();
        listeners.forEach(l -> l.optionsChanged(instanceOptions));
    }
}