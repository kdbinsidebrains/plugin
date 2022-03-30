package org.kdb.inside.brains.view.console;

import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

public class ConsoleOptionsPanel extends JPanel {
    private final JBCheckBox enlistArrays = new JBCheckBox("Enlist one item lists");
    private final JBCheckBox wrapString = new JBCheckBox("Wrap string with double quotes");
    private final JBCheckBox prefixSymbols = new JBCheckBox("Prefix symbols with grave accent");
    private final JBCheckBox showGrid = new JBCheckBox("Show table grid");
    private final JBCheckBox striped = new JBCheckBox("Stripe table rows");
    private final JBCheckBox listAsTable = new JBCheckBox("Show list as table");
    private final JBCheckBox dictAsTable = new JBCheckBox("Show dict as table");
    private final JBIntSpinner floatPrecisionEditor = new JBIntSpinner(7, 0, 20);

    public ConsoleOptionsPanel() {
        super(new BorderLayout());

        enlistArrays.setToolTipText("If enabled - an one element list will be shown as 'enlist'; comma is used otherwise.");

        final var formBuilder = FormBuilder.createFormBuilder();
        formBuilder.addComponent(showGrid);
        formBuilder.addComponent(striped);
        formBuilder.addComponent(enlistArrays);
        formBuilder.addComponent(wrapString);
        formBuilder.addComponent(prefixSymbols);
        formBuilder.addComponent(listAsTable);
        formBuilder.addComponent(dictAsTable);
        formBuilder.addLabeledComponent("Float precision: ", floatPrecisionEditor);

        add(formBuilder.getPanel());
    }


    public ConsoleOptions getConsoleOptions() {
        final ConsoleOptions consoleOptions = new ConsoleOptions();
        consoleOptions.setEnlistArrays(enlistArrays.isSelected());
        consoleOptions.setFloatPrecision(floatPrecisionEditor.getNumber());
        consoleOptions.setWrapStrings(wrapString.isSelected());
        consoleOptions.setPrefixSymbols(prefixSymbols.isSelected());
        consoleOptions.setStriped(striped.isSelected());
        consoleOptions.setShowGrid(showGrid.isSelected());
        consoleOptions.setListAsTable(listAsTable.isSelected());
        consoleOptions.setDictAsTable(dictAsTable.isSelected());
        return consoleOptions;
    }

    public void setConsoleOptions(ConsoleOptions consoleOptions) {
        floatPrecisionEditor.setNumber(consoleOptions.getFloatPrecision());
        enlistArrays.setSelected(consoleOptions.isEnlistArrays());
        wrapString.setSelected(consoleOptions.isWrapStrings());
        prefixSymbols.setSelected(consoleOptions.isPrefixSymbols());
        striped.setSelected(consoleOptions.isStriped());
        showGrid.setSelected(consoleOptions.isShowGrid());
        listAsTable.setSelected(consoleOptions.isListAsTable());
        dictAsTable.setSelected(consoleOptions.isDictAsTable());
    }
}
