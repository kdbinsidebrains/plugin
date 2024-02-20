package org.kdb.inside.brains.view.console;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

public class ConsoleOptionsPanel extends JPanel {
    private final JBCheckBox enlistArrays = new JBCheckBox("Enlist one item lists");
    private final JBCheckBox wrapString = new JBCheckBox("Wrap string with double quotes");
    private final JBCheckBox prefixSymbols = new JBCheckBox("Prefix symbols with grave accent");
    private final JBCheckBox listAsTable = new JBCheckBox("Show list as table");
    private final JBCheckBox dictAsTable = new JBCheckBox("Show dict as table");
    private final JBCheckBox consoleBackground = new JBCheckBox("Use an instance color for console background");
    private final JBCheckBox clearTableResult = new JBCheckBox("Clear 'Table Result' if result is not a table");
    private final ComboBox<ConsoleSplitType> splitTypes = new ComboBox<>(ConsoleSplitType.values());

    public ConsoleOptionsPanel() {
        super(new BorderLayout());

        enlistArrays.setToolTipText("If enabled - an one element list will be shown as 'enlist'; comma is used otherwise.");

        final var formBuilder = FormBuilder.createFormBuilder();
        formBuilder.addComponent(listAsTable);
        formBuilder.addComponent(dictAsTable);
        formBuilder.addComponent(enlistArrays);
        formBuilder.addComponent(wrapString);
        formBuilder.addComponent(prefixSymbols);
        formBuilder.addComponent(clearTableResult);
        formBuilder.addComponent(consoleBackground);

        createSplitTypes(formBuilder);

        add(formBuilder.getPanel());
    }

    private void createSplitTypes(FormBuilder formBuilder) {
        splitTypes.setEditable(false);
        splitTypes.setSelectedItem(ConsoleSplitType.NO);
        splitTypes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((ConsoleSplitType) value).getLabel(), index, isSelected, cellHasFocus);
            }
        });

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(new JBLabel("Default console and results tabs splitting: "));
        p.add(splitTypes);

        formBuilder.addComponent(p);
    }

    public ConsoleOptions getOptions() {
        final ConsoleOptions consoleOptions = new ConsoleOptions();
        consoleOptions.setEnlistArrays(enlistArrays.isSelected());
        consoleOptions.setWrapStrings(wrapString.isSelected());
        consoleOptions.setPrefixSymbols(prefixSymbols.isSelected());
        consoleOptions.setListAsTable(listAsTable.isSelected());
        consoleOptions.setDictAsTable(dictAsTable.isSelected());
        consoleOptions.setSplitType(splitTypes.getItem());
        consoleOptions.setConsoleBackground(consoleBackground.isSelected());
        consoleOptions.setClearTableResult(clearTableResult.isSelected());
        return consoleOptions;
    }

    public void setOptions(ConsoleOptions consoleOptions) {
        enlistArrays.setSelected(consoleOptions.isEnlistArrays());
        wrapString.setSelected(consoleOptions.isWrapStrings());
        prefixSymbols.setSelected(consoleOptions.isPrefixSymbols());
        listAsTable.setSelected(consoleOptions.isListAsTable());
        dictAsTable.setSelected(consoleOptions.isDictAsTable());
        splitTypes.setItem(consoleOptions.getSplitType());
        consoleBackground.setSelected(consoleOptions.isConsoleBackground());
        clearTableResult.setSelected(consoleOptions.isClearTableResult());
    }
}
