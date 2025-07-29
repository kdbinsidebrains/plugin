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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

class ColumnsFilterPanel extends NonOpaquePanel {
    //    private final MyTableColumnModel columnModel;
    private final QColumnModel columnModel;
    private final CheckBoxList<FilterColumn> columnsFilterList;

    public ColumnsFilterPanel(QTable myTable) {
        super(new BorderLayout());
        this.columnModel = myTable.getColumnModel();

//        final QTableModel model = (QTableModel) table.getModel();
//        this.columnModel = (MyTableColumnModel) table.getColumnModel();

//        myTable.addPropertyChangeListener("model", this::modelChanged);
//        final JTableHeader header = myTable.getTableHeader();


        columnsFilterList = new CheckBoxList<>() {
            @Override
            protected void doCopyToClipboardAction() {
                ArrayList<String> selected = new ArrayList<>();
                for (int index : getSelectedIndices()) {
                    final FilterColumn itemAt = getItemAt(index);
                    if (itemAt != null) {
                        selected.add(itemAt.name);
                    }
                }

                if (!selected.isEmpty()) {
                    String text = StringUtil.join(selected, "\n");
                    CopyPasteManager.getInstance().setContents(new StringSelection(text));
                }
            }

            @Override
            protected @Nls @Nullable String getSecondaryText(int index) {
                final FilterColumn item = getItemAt(index);
                if (item != null) {
                    return item.type;
                }
                return null;
            }
        };

        columnsFilterList.setCheckBoxListListener((index, value) -> {
            final FilterColumn col = columnsFilterList.getItemAt(index);
            if (col == null) {
                return;
            }
            changeColumnState(col, value);
        });

        myTable.getColumnModel().addColumnModelListener(new TheTableColumnModelListener());

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new SelectUnselectAction("Select All", "Select all columns", KdbIcons.Console.SelectAll, true));
        group.add(new SelectUnselectAction("Unselect All", "Unselect all columns", KdbIcons.Console.UnselectAll, false));

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

    private void invalidateFilter() {
        columnsFilterList.clear();

        final List<TableColumn> columns = columnModel.getColumns(true);
        for (TableColumn column : columns) {
            final QColumnInfo info = columnModel.getColumnInfo(column);
            final FilterColumn item = new FilterColumn(info.getName(), info.getColumnType().getTypeName(), column);
            columnsFilterList.addItem(item, item.name, columnModel.isVisible(column));
        }
    }

    private void changeColumnState(FilterColumn col, boolean visible) {
        columnModel.setVisible(col.column, visible);
    }

    @Override
    public JComponent getTargetComponent() {
        return columnsFilterList;
    }

    public void destroy() {
//        columnModel.reset();
    }

    record FilterColumn(String name, String type, TableColumn column) {
    }

    private class SelectUnselectAction extends DumbAwareAction {
        private final boolean selected;

        public SelectUnselectAction(String text, String description, Icon icon, boolean selected) {
            super(text, description, icon);
            this.selected = selected;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            for (FilterColumn item : columnsFilterList.getAllItems()) {
                columnsFilterList.setItemSelected(item, selected);
                changeColumnState(item, true);
            }
            columnsFilterList.repaint();
        }
    }

    private class TheTableColumnModelListener implements TableColumnModelListener {
        @Override
        public void columnAdded(TableColumnModelEvent e) {

        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {

        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {

        }
    }
}
