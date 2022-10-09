package org.kdb.inside.brains.view.console.table;

import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class ColumnsFilterPanel extends NonOpaquePanel {
    private final CheckBoxList<TableColumn> columnsFilterList;
    private final Map<TableColumn, ColumnWidth> columnSizes = new HashMap<>();

    public ColumnsFilterPanel(JTable table) {
        super(new BorderLayout());

        columnsFilterList = new CheckBoxList<>();
        columnsFilterList.setCheckBoxListListener((index, value) -> {
            final TableColumn col = columnsFilterList.getItemAt(index);
            if (col == null) {
                return;
            }

            changeColumnState(col, value);
        });

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Select All", "Select all columns", KdbIcons.Console.SelectAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
                while (columns.hasMoreElements()) {
                    final TableColumn item = columns.nextElement();
                    columnsFilterList.setItemSelected(item, true);
                    changeColumnState(item, true);
                }
                columnsFilterList.repaint();
            }
        });
        group.add(new AnAction("Unselect All", "Unselect all columns", KdbIcons.Console.UnselectAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
                while (columns.hasMoreElements()) {
                    final TableColumn item = columns.nextElement();
                    columnsFilterList.setItemSelected(item, false);
                    changeColumnState(item, false);
                }
                columnsFilterList.repaint();
            }
        });

        final ActionToolbar filterToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.FilterToolbar", group, true);
        filterToolbar.setTargetComponent(columnsFilterList);
        add(filterToolbar.getComponent(), BorderLayout.NORTH);
        add(ScrollPaneFactory.createScrollPane(columnsFilterList, true), BorderLayout.CENTER);

        updateTable(table);

        new ListSpeedSearch<>(columnsFilterList, AbstractButton::getText);

        final Dimension s = getMinimumSize();
        s.width = s.width + 155;
        setMinimumSize(s);
    }

    private void changeColumnState(TableColumn col, boolean enabled) {
        if (enabled) {
            final ColumnWidth remove = columnSizes.remove(col);
            if (remove != null) {
                remove.restore(col);
            }
        } else {
            if (!columnSizes.containsKey(col)) {
                columnSizes.put(col, new ColumnWidth(col));
            }
        }
    }

    public void updateTable(JTable table) {
        if (isTheSameTable(table)) {
            final TableColumnModel columnModel = table.getColumnModel();
            final int cnt = columnModel.getColumnCount();
            for (int i = 0; i < cnt; i++) {
                final TableColumn column = columnModel.getColumn(i);
                final TableColumn old = columnsFilterList.getItemAt(i);
                if (old != null) {
                    columnsFilterList.updateItem(old, column, String.valueOf(column.getHeaderValue()));
                    final ColumnWidth remove = columnSizes.remove(old);
                    if (remove != null) {
                        columnSizes.put(column, new ColumnWidth(column));
                    }
                }
            }
        } else {
            columnSizes.clear();
            columnsFilterList.clear();
            final Enumeration<TableColumn> columns = table.getColumnModel().getColumns();
            while (columns.hasMoreElements()) {
                final TableColumn tableColumn = columns.nextElement();
                columnsFilterList.addItem(tableColumn, String.valueOf(tableColumn.getHeaderValue()), true);
            }
        }
    }

    private boolean isTheSameTable(JTable table) {
        final TableColumnModel tm = table.getColumnModel();
        final ListModel<JCheckBox> lm = columnsFilterList.getModel();
        if (tm.getColumnCount() != lm.getSize()) {
            return false;
        }

        int c = tm.getColumnCount();
        for (int i = 0; i < c; i++) {
            final Object tn = tm.getColumn(i).getHeaderValue();
            final TableColumn itemAt = columnsFilterList.getItemAt(i);
            if (itemAt == null) {
                return false;
            }
            final Object lv = itemAt.getHeaderValue();
            if (!Objects.equals(lv, tn)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public JComponent getTargetComponent() {
        return columnsFilterList;
    }

    public void destroy() {
        columnSizes.forEach((c, w) -> w.restore(c));
        columnSizes.clear();
    }

    private static class ColumnWidth {
        private final int min;
        private final int max;
        private final int pref;

        public ColumnWidth(TableColumn c) {
            this.min = c.getMinWidth();
            this.max = c.getMaxWidth();
            this.pref = c.getPreferredWidth();

            c.setMinWidth(0);
            c.setMaxWidth(0);
            c.setPreferredWidth(0);
        }

        void restore(TableColumn c) {
            c.setMinWidth(min);
            c.setMaxWidth(max);
            c.setPreferredWidth(pref);
        }
    }
}
