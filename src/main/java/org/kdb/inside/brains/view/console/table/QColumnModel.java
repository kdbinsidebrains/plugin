package org.kdb.inside.brains.view.console.table;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// Optimized version with visibility supporting based on QTableColumn class
public class QColumnModel implements TableColumnModel {
    private int columnMargin;
    private int totalColumnWidth;
    private boolean columnSelectionAllowed;

    private final List<QColumnView> allColumns = new ArrayList<>();
    private final List<QColumnView> visibleColumns = new ArrayList<>();

    private final ChangeEvent changeEvent = new ChangeEvent(this);
    private final ListSelectionModel selectionModel = new DefaultListSelectionModel();

    private final List<TableColumnModelListener> listeners = new CopyOnWriteArrayList<>();
    private final ColumnPropertyChangeListener columnPropertyChangeListener = new ColumnPropertyChangeListener();

    public QColumnModel() {
        columnMargin = 1;
        selectionModel.addListSelectionListener(new TheListSelectionListener());

        invalidateWidthCache();
    }

    @Override
    public void addColumnModelListener(TableColumnModelListener x) {
        if (x != null) {
            listeners.add(x);
        }
    }

    @Override
    public void removeColumnModelListener(TableColumnModelListener x) {
        if (x != null) {
            listeners.remove(x);
        }
    }

    @Override
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void setSelectionModel(ListSelectionModel newModel) {
        throw new UnsupportedOperationException("Selection model can't be changed");
    }

    @Override
    public int[] getSelectedColumns() {
        return selectionModel.getSelectedIndices();
    }

    @Override
    public int getSelectedColumnCount() {
        return selectionModel.getSelectedItemsCount();
    }

    @Override
    public boolean getColumnSelectionAllowed() {
        return columnSelectionAllowed;
    }

    @Override
    public void setColumnSelectionAllowed(boolean flag) {
        columnSelectionAllowed = flag;
    }

    @Override
    public int getColumnMargin() {
        return columnMargin;
    }

    @Override
    public void setColumnMargin(int newMargin) {
        if (newMargin != columnMargin) {
            columnMargin = newMargin;
            fireColumnMarginChanged();
        }
    }

    @Override
    public void addColumn(TableColumn aColumn) {
        throw new UnsupportedOperationException("Use #syncModel instead");
    }

    @Override
    public void removeColumn(TableColumn column) {
        throw new UnsupportedOperationException("Use #syncModel instead");
    }

    /**
     * Original schema columns in sorted mode
     */
    public List<QColumnView> getSchemaColumns() {
        final List<QColumnView> res = new ArrayList<>(allColumns);
        res.sort(Comparator.comparingInt(TableColumn::getModelIndex));
        return res;
    }

    @Override
    public QColumnView getColumn(int columnIndex) {
        return visibleColumns.get(columnIndex);
    }

    @Override
    public int getColumnIndex(Object identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier is null");
        }

        int index = -1;
        for (QColumnView column : visibleColumns) {
            index++;
            if (identifier.equals(column.getIdentifier())) {
                return index;
            }
        }
        throw new IllegalArgumentException("Identifier not found");
    }

    @Override
    public int getColumnIndexAtX(int x) {
        if (x < 0) {
            return -1;
        }

        int index = -1;
        for (QColumnView column : visibleColumns) {
            index++;
            x = x - column.getWidth();
            if (x < 0) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public int getColumnCount() {
        return visibleColumns.size();
    }

    @Override
    public int getTotalColumnWidth() {
        if (totalColumnWidth == -1) {
            totalColumnWidth = visibleColumns.stream().mapToInt(TableColumn::getWidth).sum();
        }
        return totalColumnWidth;
    }

    @Override
    public Enumeration<TableColumn> getColumns() {
        return new Enumeration<>() {
            private final Iterator<QColumnView> i = visibleColumns.iterator();

            public boolean hasMoreElements() {
                return i.hasNext();
            }

            public TableColumn nextElement() {
                return i.next();
            }
        };
    }

    public void syncModel(QTableModel model) {
        final List<QColumnView> oldModel = new ArrayList<>(allColumns);

        allColumns.forEach(c -> c.removePropertyChangeListener(columnPropertyChangeListener));

        allColumns.clear();
        visibleColumns.clear();
        selectionModel.clearSelection(); // sure?

        final QColumnInfo[] infos = model.getColumnInfos();
        if (isSameModel(oldModel, infos)) {
            for (QColumnView oldC : oldModel) {
                final int i = oldC.getModelIndex();
                final QColumnView c = new QColumnView(i, infos[i]);
                c.setWidth(oldC.getWidth());
                c.setVisible(oldC.isVisible());
                c.setPreferredWidth(oldC.getPreferredWidth());
                c.addPropertyChangeListener(columnPropertyChangeListener);
                allColumns.add(c);
            }
        } else {
            for (int i = 0; i < infos.length; i++) {
                final QColumnView c = new QColumnView(i, infos[i]);
                c.addPropertyChangeListener(columnPropertyChangeListener);
                allColumns.add(c);
            }
        }

        allColumns.stream().filter(QColumnView::isVisible).forEach(visibleColumns::add);

        invalidateWidthCache();
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        final int columnCount = getColumnCount();
        if ((columnIndex < 0) || (newIndex < 0) || (columnIndex >= columnCount) || (newIndex >= columnCount)) {
            throw new IllegalArgumentException("moveColumn() - Index out of range");
        }

        final TableColumnModelEvent event = new TableColumnModelEvent(this, columnIndex, newIndex);
        if (columnIndex == newIndex) {
            listeners.forEach(l -> l.columnMoved(event));
            return;
        }

        final QColumnView aColumn = visibleColumns.get(columnIndex);
        final int allNewIndex = allColumns.indexOf(visibleColumns.get(newIndex));

        allColumns.remove(aColumn);
        allColumns.add(allNewIndex, aColumn);

        visibleColumns.remove(aColumn);
        visibleColumns.add(newIndex, aColumn);

        boolean selected = selectionModel.isSelectedIndex(columnIndex);
        selectionModel.removeIndexInterval(columnIndex, columnIndex);
        selectionModel.insertIndexInterval(newIndex, 1, true);
        if (selected) {
            selectionModel.addSelectionInterval(newIndex, newIndex);
        } else {
            selectionModel.removeSelectionInterval(newIndex, newIndex);
        }

        listeners.forEach(l -> l.columnMoved(event));
    }

    private void changeColumnVisibility(QColumnView aColumn, boolean visible) {
        if (!allColumns.contains(aColumn)) {
            return;
        }

        aColumn.setVisible(visible);
        invalidateWidthCache();

        if (visible) {
            int newIndex = -1;
            for (int i = allColumns.indexOf(aColumn); i >= 0 && newIndex < 0; i--) {
                newIndex = visibleColumns.indexOf(allColumns.get(i));
            }
            newIndex++;
            visibleColumns.add(newIndex, aColumn);
            final TableColumnModelEvent e = new TableColumnModelEvent(this, newIndex, newIndex);
            listeners.forEach(l -> l.columnAdded(e));
        } else {
            final int oldIndex = visibleColumns.indexOf(aColumn);
            if (oldIndex >= 0) {
                visibleColumns.remove(oldIndex);
                selectionModel.removeIndexInterval(oldIndex, oldIndex);
                final TableColumnModelEvent e = new TableColumnModelEvent(this, oldIndex, oldIndex);
                listeners.forEach(l -> l.columnRemoved(e));
            }
        }
    }

    private boolean isSameModel(List<QColumnView> oldModel, QColumnInfo[] infos) {
        if (oldModel.size() != infos.length) {
            return false;
        }
        for (QColumnView c : oldModel) {
            final QColumnInfo oldInfo = c.getColumnInfo();
            final QColumnInfo newInfo = infos[c.getModelIndex()];
            if (!oldInfo.equals(newInfo)) {
                return false;
            }
        }
        return true;
    }

    private class TheListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            listeners.forEach(l -> l.columnSelectionChanged(e));
        }
    }

    private void invalidateWidthCache() {
        totalColumnWidth = -1;
    }

    private void fireColumnMarginChanged() {
        listeners.forEach(l -> l.columnMarginChanged(changeEvent));
    }

    private class ColumnPropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();

            if (Objects.equals(name, "width") || Objects.equals(name, "preferredWidth")) {
                invalidateWidthCache();
                fireColumnMarginChanged();
            } else if (Objects.equals(name, "visible")) {
                changeColumnVisibility((QColumnView) evt.getSource(), evt.getNewValue() == Boolean.TRUE);
            }
        }
    }
}