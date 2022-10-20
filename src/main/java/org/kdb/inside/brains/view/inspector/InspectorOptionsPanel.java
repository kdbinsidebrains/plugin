package org.kdb.inside.brains.view.inspector;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

public class InspectorOptionsPanel extends JPanel {
    private final JBCheckBox autoScanInstance = new JBCheckBox("Auto-scan an instance after connection");

    public InspectorOptionsPanel() {
        super(new BorderLayout());

        final var formBuilder = FormBuilder.createFormBuilder();
        formBuilder.addComponent(autoScanInstance);

        add(formBuilder.getPanel());
    }

    public InspectorOptions getOptions() {
        final InspectorOptions options = new InspectorOptions();
        options.setScanOnConnect(autoScanInstance.isSelected());
        return options;
    }

    public void setOptions(InspectorOptions options) {
        autoScanInstance.setSelected(options.isScanOnConnect());
    }
}
