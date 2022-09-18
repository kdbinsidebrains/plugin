package org.kdb.inside.brains.view.console;

import com.intellij.openapi.Disposable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/*
 *	Use a JTable as a renderer for row numbers of a given main table.
 *  This table must be added to the row header of the scrollpane that
 *  contains the main table.
 */
class RowNumberTable extends JTable implements Disposable {
    private final JTable main;
    private final RowNumberRenderer cellRenderer;

    private final TheListener listener = new TheListener();

    public RowNumberTable(JTable table) {
        main = table;
        main.addPropertyChangeListener(listener);
        main.getModel().addTableModelListener(listener);

        setFocusable(false);
        setAutoCreateColumnsFromModel(false);
        setSelectionModel(new ListSelectionModelWrapper(main));

        final TableColumn column = new TableColumn();
        column.setHeaderValue("#");
        addColumn(column);

        cellRenderer = new RowNumberRenderer(main);
        column.setCellRenderer(cellRenderer);

        recalculateWidth(main.getModel());

    }

    private void recalculateWidth(TableModel model) {
        final FontMetrics fm = cellRenderer.getFontMetrics(cellRenderer.selected);
        getColumnModel().getColumn(0).setPreferredWidth(fm.stringWidth(String.valueOf(model.getRowCount()) + getIntercellSpacing().width * 2));

        final Dimension preferredSize = getPreferredSize();
        setPreferredScrollableViewportSize(preferredSize);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        final Component c = getParent();
        if (c instanceof JViewport) {
            JViewport viewport = (JViewport) c;
            viewport.addChangeListener(listener);
        }
    }

    /*
     *  Delegate method to main table
     */
    @Override
    public int getRowCount() {
        return main.getRowCount();
    }

    @Override
    public int getRowHeight(int row) {
        int rowHeight = main.getRowHeight(row);
        if (rowHeight != super.getRowHeight(row)) {
            super.setRowHeight(row, rowHeight);
        }
        return rowHeight;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return row;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
    }

    @Override
    public void dispose() {
        main.removePropertyChangeListener(listener);
        main.getModel().removeTableModelListener(listener);

        final Component c = getParent();
        if (c instanceof JViewport) {
            JViewport viewport = (JViewport) c;
            viewport.removeChangeListener(listener);
        }
    }

    private static class RowNumberRenderer extends DefaultTableCellRenderer {
        private final Font normal;
        private final Font selected;

        private final Border selectedBorder;
        private final Border unselectedBorder;

        public RowNumberRenderer(JTable table) {
            setHorizontalAlignment(JLabel.CENTER);

            unselectedBorder = JBUI.Borders.emptyRight(1);
            selectedBorder = JBUI.Borders.customLineRight(table.getSelectionBackground());

            final JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                normal = header.getFont();
            } else {
                normal = getFont();
            }
            selected = normal.deriveFont(Font.BOLD);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setFont(isSelected ? selected : normal);
            setBorder(isSelected ? selectedBorder : unselectedBorder);
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    private class TheListener implements ChangeListener, PropertyChangeListener, TableModelListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            //  Keep the row table in sync with the main table
            if ("selectionModel".equals(e.getPropertyName())) {
                setSelectionModel(main.getSelectionModel());
            }

            if ("rowHeight".equals(e.getPropertyName())) {
                repaint();
            }

            if ("model".equals(e.getPropertyName())) {
                final TableModel oldModel = (TableModel) e.getOldValue();
                if (oldModel != null) {
                    oldModel.removeTableModelListener(listener);
                }

                final TableModel newModel = (TableModel) e.getNewValue();
                if (newModel != null) {
                    newModel.addTableModelListener(listener);
                    recalculateWidth(newModel);
                }
                revalidate();
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JViewport viewport = (JViewport) e.getSource();
            JScrollPane scrollPane = (JScrollPane) viewport.getParent();
            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            recalculateWidth((TableModel) e.getSource());
            revalidate();
        }
    }

    private static class ListSelectionModelWrapper implements ListSelectionModel {
        private final JTable main;
        private final ListSelectionModel mainModel;

        public ListSelectionModelWrapper(JTable main) {
            this.main = main;
            mainModel = main.getSelectionModel();
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            mainModel.addSelectionInterval(index0, index1);
            invalidateColsSelection();
        }

        @Override
        public void removeSelectionInterval(int index0, int index1) {
            mainModel.removeSelectionInterval(index0, index1);
            invalidateColsSelection();
        }

        @Override
        public void insertIndexInterval(int index, int length, boolean before) {
            mainModel.insertIndexInterval(index, length, before);
            invalidateColsSelection();
        }

        @Override
        public void removeIndexInterval(int index0, int index1) {
            mainModel.removeIndexInterval(index0, index1);
            invalidateColsSelection();
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            mainModel.setSelectionInterval(index0, index1);
            invalidateColsSelection();
        }

        @Override
        public int getMinSelectionIndex() {
            return mainModel.getMinSelectionIndex();
        }

        @Override
        public int getMaxSelectionIndex() {
            return mainModel.getMaxSelectionIndex();
        }

        @Override
        public boolean isSelectedIndex(int index) {
            return mainModel.isSelectedIndex(index);
        }

        @Override
        public int getAnchorSelectionIndex() {
            return mainModel.getAnchorSelectionIndex();
        }

        @Override
        public void setAnchorSelectionIndex(int index) {
            mainModel.setAnchorSelectionIndex(index);
        }

        @Override
        public int getLeadSelectionIndex() {
            return mainModel.getLeadSelectionIndex();
        }

        @Override
        public void setLeadSelectionIndex(int index) {
            mainModel.setLeadSelectionIndex(index);
        }

        @Override
        public void clearSelection() {
            mainModel.clearSelection();
        }

        @Override
        public boolean isSelectionEmpty() {
            return mainModel.isSelectionEmpty();
        }

        @Override
        public boolean getValueIsAdjusting() {
            return mainModel.getValueIsAdjusting();
        }

        @Override
        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            mainModel.setValueIsAdjusting(valueIsAdjusting);
        }

        @Override
        public int getSelectionMode() {
            return mainModel.getSelectionMode();
        }

        @Override
        public void setSelectionMode(int selectionMode) {
            mainModel.setSelectionMode(selectionMode);
        }

        @Override
        public void addListSelectionListener(ListSelectionListener x) {
            mainModel.addListSelectionListener(x);
        }

        @Override
        public void removeListSelectionListener(ListSelectionListener x) {
            mainModel.removeListSelectionListener(x);
        }

        private void invalidateColsSelection() {
            final int columnCount = main.getColumnCount();
            final ListSelectionModel colSelectionModel = main.getColumnModel().getSelectionModel();
            if (columnCount != colSelectionModel.getSelectedItemsCount()) {
                colSelectionModel.setSelectionInterval(0, columnCount - 1);
            }
        }
    }
}