package org.kdb.inside.brains.view.console;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;
import java.math.RoundingMode;

public class NumericalOptionsPanel extends JPanel {
    private final ComboBox<RoundingItem> roungingComboBox = new ComboBox<>();
    private final JBCheckBox scientificNotation = new JBCheckBox("Show decimals in scientific notation (1.23e+014)");
    private final JBIntSpinner floatPrecisionEditor = new JBIntSpinner(7, 0, NumericalOptions.MAX_DECIMAL_PRECISION);

    public NumericalOptionsPanel() {
        super(new BorderLayout());

        roungingComboBox.addItem(new RoundingItem(RoundingMode.UP, "Up", "Rounding mode to round away from zero."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.DOWN, "Down", "Rounding mode to round towards zero."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.CEILING, "Ceiling", "Rounding mode to round towards positive infinity."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.FLOOR, "Floor", "Rounding mode to round towards negative infinity."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.HALF_UP, "Half Up", "Rounding mode to round towards \"nearest neighbor\" unless both neighbors are equidistant, in which case round up."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.HALF_DOWN, "Half Down", "Rounding mode to round towards \"nearest neighbor\" unless both neighbors are equidistant, in which case round down."));
        roungingComboBox.addItem(new RoundingItem(RoundingMode.HALF_EVEN, "Half Even", "Rounding mode to round towards the \"nearest neighbor\" unless both neighbors are equidistant, in which case, round towards the even neighbor."));
        roungingComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((RoundingItem) value).name, index, isSelected, cellHasFocus);
            }
        });

        final var formBuilder = FormBuilder.createFormBuilder();
        addPrecision(formBuilder);
        addRoundingMode(formBuilder);
        addScientificNotation(formBuilder);

        add(formBuilder.getPanel());
    }

    public NumericalOptions getOptions() {
        final NumericalOptions options = new NumericalOptions();
        options.setRoundingMode(roungingComboBox.getItem().mode());
        options.setFloatPrecision(floatPrecisionEditor.getNumber());
        options.setScientificNotation(scientificNotation.isSelected());
        return options;
    }

    public void setOptions(NumericalOptions options) {
        roungingComboBox.setSelectedIndex(findRoundingItem(options.getRoundingMode()));
        floatPrecisionEditor.setNumber(options.getFloatPrecision());
        scientificNotation.setSelected(options.isScientificNotation());
    }

    private int findRoundingItem(RoundingMode mode) {
        final int itemCount = roungingComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (mode == roungingComboBox.getItemAt(i).mode) {
                return i;
            }
        }
        return -1;
    }

    private void addPrecision(FormBuilder formBuilder) {
        formBuilder.addLabeledComponent("Float precision: ", floatPrecisionEditor);
    }

    private void addRoundingMode(FormBuilder formBuilder) {
        final StringBuilder b = new StringBuilder("<html>");
        final int itemCount = roungingComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            final RoundingItem itemAt = roungingComboBox.getItemAt(i);
            b.append("<strong>").append(itemAt.name()).append(": ").append("</strong>").append(itemAt.desc());
            b.append("<br><br>");
        }
        b.append("</html>");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(new JBLabel("Rounding mode: "));
        p.add(roungingComboBox);
        p.add(Box.createHorizontalStrut(5));
        p.add(ContextHelpLabel.create(b.toString()));
        formBuilder.addComponent(p);
    }

    private void addScientificNotation(FormBuilder formBuilder) {
        final ContextHelpLabel infoLabel = ContextHelpLabel.create("<html>Display decimal numbers less or equal 10<sup>-5</sup> or more or equal 10<sup>7</sup></html> in scientific notation (like 1.2345e-003)");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(scientificNotation);
        p.add(Box.createHorizontalStrut(5));
        p.add(infoLabel);
        formBuilder.addComponent(p);
    }

    record RoundingItem(RoundingMode mode, String name, String desc) {
    }
}