package org.kdb.inside.brains.view.console;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;
import java.awt.*;

public class TableOptionsPanel extends JPanel {
    private final JBCheckBox showGrid = new JBCheckBox("Show table grid");
    private final JBCheckBox striped = new JBCheckBox("Stripe table rows");
    private final JBCheckBox indexColumn = new JBCheckBox("Show index column");
    private final JBCheckBox thousandsColumn = new JBCheckBox("Show thousands separator");
    private final JBCheckBox expandList = new JBCheckBox("Vector");
    private final JBCheckBox expandDict = new JBCheckBox("Dictionary");
    private final JBCheckBox expandFlip = new JBCheckBox("Table");
    private final JBCheckBox xmasKeyColumn = new JBCheckBox("Show XMas key column");

    public TableOptionsPanel() {
        super(new BorderLayout());

        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder.addComponent(showGrid);
        formBuilder.addComponent(striped);
        formBuilder.addComponent(indexColumn);
        formBuilder.addComponent(thousandsColumn);
        addExpandPanel(formBuilder);

        add(formBuilder.getPanel());
    }

    private void addExpandPanel(FormBuilder formBuilder) {
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.add(expandList);
        p.add(expandDict);
        p.add(expandFlip);

        formBuilder.addComponent(new JBLabel("Expand value by double click: "));
        formBuilder.setFormLeftIndent(20);
        formBuilder.addComponent(p);
        formBuilder.setFormLeftIndent(0);
    }

    public TableOptions getOptions() {
        final TableOptions options = new TableOptions();
        options.setStriped(striped.isSelected());
        options.setShowGrid(showGrid.isSelected());
        options.setIndexColumn(indexColumn.isSelected());
        options.setExpandList(expandList.isSelected());
        options.setExpandDict(expandDict.isSelected());
        options.setExpandTable(expandFlip.isSelected());
        options.setXmasKeyColumn(xmasKeyColumn.isSelected());
        options.setThousandsSeparator(thousandsColumn.isSelected());
        return options;
    }

    public void setOptions(TableOptions options) {
        striped.setSelected(options.isStriped());
        showGrid.setSelected(options.isShowGrid());
        indexColumn.setSelected(options.isIndexColumn());
        expandList.setSelected(options.isExpandList());
        expandDict.setSelected(options.isExpandDict());
        expandFlip.setSelected(options.isExpandTable());
        xmasKeyColumn.setSelected(options.isXmasKeyColumn());
        thousandsColumn.setSelected(options.isThousandsSeparator());
    }
}
