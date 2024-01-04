package org.kdb.inside.brains.view.console.table;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ColumnsFilterPanel extends NonOpaquePanel {
    private final MyTableColumnModel columnModel;
    private final CheckBoxList<TableColumn> columnsFilterList;

    public ColumnsFilterPanel(JTable table) {
        super(new BorderLayout());
        this.columnModel = (MyTableColumnModel) table.getColumnModel();

        columnsFilterList = new CheckBoxList<>() {
            @Override
            protected void doCopyToClipboardAction() {
                ArrayList<String> selected = new ArrayList<>();
                for (int index : getSelectedIndices()) {
                    final TableColumn itemAt = columnsFilterList.getItemAt(index);
                    if (itemAt != null) {
                        String text = String.valueOf(itemAt.getHeaderValue());
                        if (text != null) {
                            selected.add(text);
                        }
                    }
                }

                if (!selected.isEmpty()) {
                    String text = StringUtil.join(selected, "\n");
                    CopyPasteManager.getInstance().setContents(new StringSelection(text));
                }
            }
        };
        columnsFilterList.setCheckBoxListListener((index, value) -> {
            final TableColumn col = columnsFilterList.getItemAt(index);
            if (col == null) {
                return;
            }
            changeColumnState(col, value);
        });

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new DumbAwareAction("Select All", "Select all columns", KdbIcons.Console.SelectAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final List<TableColumn> columns = columnModel.getColumns(true);
                for (TableColumn column : columns) {
                    columnsFilterList.setItemSelected(column, true);
                    changeColumnState(column, true);
                }
                columnsFilterList.repaint();
            }
        });
        group.add(new DumbAwareAction("Unselect All", "Unselect all columns", KdbIcons.Console.UnselectAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final List<TableColumn> columns = columnModel.getColumns(true);
                for (TableColumn column : columns) {
                    columnsFilterList.setItemSelected(column, false);
                    changeColumnState(column, false);
                }
                columnsFilterList.repaint();
            }
        });

        final ActionToolbar filterToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.FilterToolbar", group, true);
        filterToolbar.setTargetComponent(columnsFilterList);
        add(filterToolbar.getComponent(), BorderLayout.NORTH);
        add(ScrollPaneFactory.createScrollPane(columnsFilterList, true), BorderLayout.CENTER);

        invalidateFilter();

        new ListSpeedSearch<>(columnsFilterList, AbstractButton::getText);

        final Dimension s = getMinimumSize();
        s.width = s.width + 155;
        setMinimumSize(s);
    }

    @NotNull
    private static Set<Object> getColumnNames(List<TableColumn> oldCols) {
        return oldCols.stream().map(TableColumn::getHeaderValue).collect(Collectors.toSet());
    }

    private void changeColumnState(TableColumn col, boolean enabled) {
        columnModel.setVisible(col, enabled);
    }

    public void invalidateFilter() {
        columnsFilterList.clear();
        final Enumeration<TableColumn> columns = columnModel.getColumns();
        while (columns.hasMoreElements()) {
            final TableColumn tableColumn = columns.nextElement();
            columnsFilterList.addItem(tableColumn, String.valueOf(tableColumn.getHeaderValue()), true);
        }
    }

    private List<TableColumn> getListItems() {
        final int size = columnsFilterList.getModel().getSize();
        final List<TableColumn> res = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            res.add(columnsFilterList.getItemAt(i));
        }
        return res;
    }

    @Override
    public JComponent getTargetComponent() {
        return columnsFilterList;
    }

    public void destroy() {
        columnModel.reset();
    }
}
