package org.kdb.inside.brains.view.console.table;

import com.intellij.openapi.Disposable;
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
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

class QSchemaViewPanel extends NonOpaquePanel implements Disposable {
    //    private final QTableColumnModel columnModel;
    private final CheckBoxList<QColumnView> columnsFilterList;

    @NotNull
    private final QTable myTable;
    private final ThePropertyChangeListener listener = new ThePropertyChangeListener();

    public QSchemaViewPanel(QTable myTable) {
        super(new BorderLayout());

        this.myTable = myTable;

        columnsFilterList = new CheckBoxList<>() {
            @Override
            protected void doCopyToClipboardAction() {
                final ArrayList<String> selected = new ArrayList<>();
                for (int index : getSelectedIndices()) {
                    final QColumnView itemAt = getItemAt(index);
                    if (itemAt != null) {
                        selected.add(itemAt.getName());
                    }
                }

                if (!selected.isEmpty()) {
                    String text = StringUtil.join(selected, "\n");
                    CopyPasteManager.getInstance().setContents(new StringSelection(text));
                }
            }

            @Override
            protected @Nls @Nullable String getSecondaryText(int index) {
                final QColumnView item = getItemAt(index);
                if (item != null) {
                    return item.getColumnType().getTypeName();
                }
                return null;
            }
        };

        columnsFilterList.setCheckBoxListListener((index, value) -> {
            final QColumnView col = columnsFilterList.getItemAt(index);
            if (col == null) {
                return;
            }
            col.setVisible(value);
        });

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new SelectUnselectAction("Select All", "Select all columns", KdbIcons.Console.SelectAll, true));
        group.add(new SelectUnselectAction("Unselect All", "Unselect all columns", KdbIcons.Console.UnselectAll, false));

        final ActionToolbar filterToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.FilterToolbar", group, true);
        filterToolbar.setTargetComponent(columnsFilterList);
        add(filterToolbar.getComponent(), BorderLayout.NORTH);
        add(ScrollPaneFactory.createScrollPane(columnsFilterList, true), BorderLayout.CENTER);

        new ListSpeedSearch<>(columnsFilterList, AbstractButton::getText);

        final Dimension s = getMinimumSize();
        s.width = s.width + 155;
        setMinimumSize(s);

        myTable.addPropertyChangeListener("columnModel", listener);
        invalidateFilter(myTable.getColumnModel());
    }

    @Override
    public JComponent getTargetComponent() {
        return columnsFilterList;
    }

    @Override
    public void dispose() {
        myTable.removePropertyChangeListener("columnModel", listener);
        columnsFilterList.clear();
    }

    private void invalidateFilter(QColumnModel columnModel) {
        columnsFilterList.clear();

        for (QColumnView column : columnModel.getSchemaColumns()) {
            columnsFilterList.addItem(column, column.getName(), column.isVisible());
        }
    }

    private class SelectUnselectAction extends DumbAwareAction {
        private final boolean selected;

        public SelectUnselectAction(String text, String description, Icon icon, boolean selected) {
            super(text, description, icon);
            this.selected = selected;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            for (QColumnView item : columnsFilterList.getAllItems()) {
                item.setVisible(selected);
                columnsFilterList.setItemSelected(item, selected);
            }
            columnsFilterList.repaint();
        }
    }

    private class ThePropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            invalidateFilter((QColumnModel) evt.getNewValue());
        }
    }
}