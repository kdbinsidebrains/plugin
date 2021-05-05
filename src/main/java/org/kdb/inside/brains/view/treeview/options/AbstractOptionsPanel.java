package org.kdb.inside.brains.view.treeview.options;

import org.kdb.inside.brains.core.InstanceOptions;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractOptionsPanel extends JPanel {
    private final List<OptionsChangedListener> listeners = new CopyOnWriteArrayList<>();

    public AbstractOptionsPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public void addOptionsChangedListener(OptionsChangedListener l) {
        if (listeners != null) {
            listeners.add(l);
        }
    }

    public void removeOptionsChangedListener(OptionsChangedListener l) {
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
