package org.kdb.inside.brains.core;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractOptionsPanel extends JPanel {
    private final List<InstanceOptionsListener> listeners = new CopyOnWriteArrayList<>();

    public AbstractOptionsPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public void addOptionsChangedListener(InstanceOptionsListener l) {
        if (listeners != null) {
            listeners.add(l);
        }
    }

    public void removeOptionsChangedListener(InstanceOptionsListener l) {
        if (listeners != null) {
            listeners.remove(l);
        }
    }

    public abstract InstanceOptions getInstanceOptions();

    protected void notifyOptionsChanged() {
        final InstanceOptions instanceOptions = getInstanceOptions();
        listeners.forEach(l -> l.optionsChanged(instanceOptions));
    }
}
