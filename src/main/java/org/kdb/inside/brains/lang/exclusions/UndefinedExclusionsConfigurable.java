package org.kdb.inside.brains.lang.exclusions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.settings.KdbConfigurable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class UndefinedExclusionsConfigurable extends KdbConfigurable {
    public static final String SETTINGS_PAGE_ID = "Kdb.Settings.UndefinedVariables";
    private static final String[] COLUMN_NAMES = {"Name", "Regex"};
    private final List<UndefinedExclusion> exclusions = new ArrayList<>();

    private final UndefinedExclusionsService service = UndefinedExclusionsService.getInstance();
    private JBTable myTable;
    private AbstractTableModel myTableModel;

    public UndefinedExclusionsConfigurable() {
        super(SETTINGS_PAGE_ID, "Undefined Variables");
    }

    @Override
    public @Nullable JComponent createComponent() {
        myTableModel = new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return COLUMN_NAMES.length;
            }

            @Override

            public String getColumnName(int column) {
                return COLUMN_NAMES[column];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : boolean.class;
            }

            @Override
            public int getRowCount() {
                return exclusions.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final UndefinedExclusion exclusion = exclusions.get(rowIndex);
                return columnIndex == 0 ? exclusion.name() : exclusion.regex();
            }
        };

        myTable = new JBTable();
        myTable.setModel(myTableModel);
        myTable.setShowColumns(true);
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final int row = myTable.getSelectedRow();
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && row != -1) {
                    addOrEdit(null, row);
                }
            }
        });

        final var formBuilder = FormBuilder.createFormBuilder();

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTable)
                .setAddAction(this::doAdd)
                .setEditAction(this::doEdit)
                .setRemoveAction(this::doRemove);

        formBuilder
                .addComponent(new TitledSeparator("Undefined Variables Exclusion List"))
                .addComponentFillVertically(decorator.createPanel(), 0)
        ;

        return formBuilder.getPanel();
    }

    private void doAdd(AnActionButton button) {
        addOrEdit(button, -1);
    }

    private void doEdit(AnActionButton button) {
        addOrEdit(button, myTable.getSelectedRow());
    }

    private void doRemove(AnActionButton button) {
        final int row = myTable.getSelectedRow();
        if (row != -1) {
            exclusions.remove(row);
            myTableModel.fireTableRowsDeleted(row, row);
        }
    }

    private void addOrEdit(@Nullable AnActionButton button, int oldIndex) {
        final Project project = button == null ? null : CommonDataKeys.PROJECT.getData(button.getDataContext());
        final UndefinedExclusion value = oldIndex == -1 ? null : exclusions.get(oldIndex);

        final UndefinedExclusion exclusion = new UndefinedExclusionDialog(project,
                n -> (value == null || !value.name().equals(n)) && UndefinedExclusion.isExcluded(n, exclusions)
        ).show(value);
        if (exclusion == null) {
            return;
        }

        if (oldIndex != -1) {
            exclusions.remove(oldIndex);
            myTableModel.fireTableRowsDeleted(oldIndex, oldIndex);
        }

        int index = Collections.binarySearch(exclusions, exclusion);
        if (index >= 0) {
            exclusions.set(index, exclusion);
            myTableModel.fireTableRowsUpdated(index, index);
        } else {
            index = -(index + 1);
            exclusions.add(index, exclusion);
            myTableModel.fireTableRowsInserted(index, index);
        }
        myTable.setRowSelectionInterval(index, index);
    }

    @Override
    public boolean isModified() {
        return !new HashSet<>(exclusions).equals(service.getExclusions());
    }

    @Override
    public void apply() throws ConfigurationException {
        service.setExclusions(exclusions);
    }

    @Override
    public void reset() {
        exclusions.clear();
        exclusions.addAll(service.getExclusions());
        Collections.sort(exclusions);
        myTableModel.fireTableDataChanged();
    }
}