package org.kdb.inside.brains.view.inspector;

import com.intellij.ide.structureView.newStructureView.TreeActionsOwner;
import org.kdb.inside.brains.view.inspector.model.InspectorTreeModel;

import java.util.HashSet;
import java.util.Set;

public class InspectorToolState implements TreeActionsOwner {
    private final Runnable runnable;

    private final Set<String> enabledActions = new HashSet<>();

    public InspectorToolState() {
        this(null);
    }

    public InspectorToolState(Runnable runnable) {
        this.runnable = runnable;
        enabledActions.add(InspectorTreeModel.SHOW_SYSTEM_NAMESPACES);
    }

    public Set<String> getEnabledActions() {
        return enabledActions;
    }

    public void setEnabledActions(Set<String> actions) {
        enabledActions.clear();
        enabledActions.addAll(actions);
    }

    @Override
    public void setActionActive(String name, boolean state) {
        if (state) {
            enabledActions.add(name);
        } else {
            enabledActions.remove(name);
        }

        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public boolean isActionActive(String name) {
        return enabledActions.contains(name);
    }

    public void copyFom(InspectorToolState state) {
        setEnabledActions(state.enabledActions);
    }
}
