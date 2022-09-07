package org.kdb.inside.brains.view.console;

import com.intellij.openapi.Disposable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
        setSelectionModel(main.getSelectionModel());

        final TableColumn column = new TableColumn();
        column.setHeaderValue("#");
        addColumn(column);

        cellRenderer = new RowNumberRenderer(main.getTableHeader());
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

        public RowNumberRenderer(JTableHeader header) {
            setHorizontalAlignment(JLabel.CENTER);

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
}