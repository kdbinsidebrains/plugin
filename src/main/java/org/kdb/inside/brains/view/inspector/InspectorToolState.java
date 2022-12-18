package org.kdb.inside.brains.view.inspector;

import org.kdb.inside.brains.view.inspector.model.InspectorTreeModel;

import java.util.HashSet;
import java.util.Set;

public class InspectorToolState {
    private boolean autoScroll = false;
    private final Set<String> enabledActions = new HashSet<>();

    public InspectorToolState() {
        enabledActions.add(InspectorTreeModel.SHOW_SYSTEM_NAMESPACES);
    }

    public Set<String> getEnabledActions() {
        return enabledActions;
    }

    public void setEnabledActions(Set<String> actions) {
        enabledActions.clear();
        enabledActions.addAll(actions);
    }

    public void setEnabled(String name, boolean state) {
        if (state) {
            enabledActions.add(name);
        } else {
            enabledActions.remove(name);
        }
    }

    public boolean isEnabled(String name) {
        return enabledActions.contains(name);
    }

    public void copyFom(InspectorToolState state) {
        setEnabledActions(state.enabledActions);
        autoScroll = state.autoScroll;
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
    }
}
