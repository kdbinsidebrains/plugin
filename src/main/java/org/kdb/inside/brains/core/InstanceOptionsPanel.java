package org.kdb.inside.brains.core;

import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.kdb.inside.brains.view.treeview.options.AbstractOptionsPanel;

import javax.swing.*;
import java.awt.*;

public class InstanceOptionsPanel extends AbstractOptionsPanel {
    private final JLabel labelText;
    private final JBCheckBox tlsCheckbox = new JBCheckBox("TLS is enabled");
    private final JBCheckBox compressionCheckbox = new JBCheckBox("Compression is enabled");
    private final JBCheckBox asynchronousCheckbox = new JBCheckBox("Asynchronous connection");
    private final JBIntSpinner timeoutSpinner = new JBIntSpinner(1000, 1000, Integer.MAX_VALUE, 10);

    public InstanceOptionsPanel() {
        super(new BorderLayout());

        final var formBuilder = FormBuilder.createFormBuilder();

        tlsCheckbox.addItemListener(e -> notifyOptionsChanged());
        compressionCheckbox.addItemListener(e -> notifyOptionsChanged());
        asynchronousCheckbox.addItemListener(e -> notifyOptionsChanged());
        timeoutSpinner.addChangeListener(e -> notifyOptionsChanged());

        labelText = new JLabel("Connection timeout, ms: ");
        formBuilder
                .addComponent(tlsCheckbox)
                .addComponent(compressionCheckbox)
                .addComponent(asynchronousCheckbox)
                .addLabeledComponent(labelText, timeoutSpinner)

                .addComponentFillVertically(new JPanel(), 0);

        add(formBuilder.getPanel());
    }

    @Override
    public InstanceOptions getInstanceOptions() {
        final var options = new InstanceOptions();
        options.setTls(tlsCheckbox.isSelected());
        options.setTimeout(timeoutSpinner.getNumber());
        options.setCompression(compressionCheckbox.isSelected());
        options.setAsynchronous(asynchronousCheckbox.isSelected());
        return options;
    }

    public void setInstanceOptions(InstanceOptions options) {
        if (options != null) {
            tlsCheckbox.setSelected(options.isTls());
            compressionCheckbox.setSelected(options.isCompression());
            asynchronousCheckbox.setSelected(options.isAsynchronous());
            timeoutSpinner.setValue(options.getTimeout());
        }
    }

    public void setEnabled(boolean b) {
        tlsCheckbox.setEnabled(b);
        compressionCheckbox.setEnabled(b);
        timeoutSpinner.setEnabled(b);
        labelText.setEnabled(b);
        asynchronousCheckbox.setEnabled(b);

        super.setEnabled(b);
    }
}
