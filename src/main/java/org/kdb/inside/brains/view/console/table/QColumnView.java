package org.kdb.inside.brains.view.console.table;

import org.kdb.inside.brains.KdbType;

import javax.swing.table.TableColumn;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class QColumnView extends TableColumn {
    private boolean visible;

    private final QColumnInfo info;

    public QColumnView(int modelIndex, QColumnInfo info) {
        super(modelIndex);
        this.info = info;
        this.visible = true;
        setHeaderValue(info.getDisplayName());
    }

    public String getName() {
        return info.getName();
    }

    public boolean isKey() {
        return info.isKey();
    }

    public KdbType getColumnType() {
        return info.getColumnType();
    }

    public Class<?> getColumnClass() {
        return info.getColumnClass();
    }

    public QColumnInfo getColumnInfo() {
        return info;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        final PropertyChangeEvent event = new PropertyChangeEvent(this, "visible", this.visible, visible);
        this.visible = visible;
        for (PropertyChangeListener propertyChangeListener : getPropertyChangeListeners()) {
            propertyChangeListener.propertyChange(event);
        }
    }
}