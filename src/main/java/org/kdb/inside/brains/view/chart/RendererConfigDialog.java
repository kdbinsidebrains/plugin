package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

import static org.kdb.inside.brains.settings.KdbConfigurable.FORM_LEFT_INDENT;

public class RendererConfigDialog extends DialogWrapper {
    private final JColorChooser colorChooser = new JColorChooser();
    private final ComboBox<String> capComboBox = new ComboBox<>(new String[]{"Butt", "Round", "Square"});
    private final ComboBox<String> joinComboBox = new ComboBox<>(new String[]{"Miter", "Round", "Bevel"});
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.2));

    public RendererConfigDialog(Component parent, RendererConfig rendererConfig) {
        super(parent, false);


        setTitle("Configure Stroke and Color");

        colorChooser.setColor(rendererConfig.color());

        final BasicStroke stroke = rendererConfig.stroke();
        widthSpinner.setValue(stroke.getLineWidth());
        capComboBox.setSelectedIndex(stroke.getEndCap());
        joinComboBox.setSelectedIndex(stroke.getLineJoin());

        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder.setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Stroke Parameters"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addLabeledComponent("Width", widthSpinner)
                .addLabeledComponent("Cap", capComboBox)
                .addLabeledComponent("Join", joinComboBox)

                .addComponent(new TitledSeparator("Color Parameters"))
                .addComponent(colorChooser)
        ;

        return formBuilder.getPanel();
    }

    public RendererConfig showAndApply() {
        if (!showAndGet()) {
            return null;
        }

        float width = ((Number) widthSpinner.getValue()).floatValue();
        final BasicStroke newStroke = new BasicStroke(width, capComboBox.getSelectedIndex(), joinComboBox.getSelectedIndex());
        return new RendererConfig(colorChooser.getColor(), newStroke);
    }
}
