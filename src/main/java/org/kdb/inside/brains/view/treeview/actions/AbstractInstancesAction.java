package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

public abstract class AbstractInstancesAction extends AnAction implements DumbAware {
    public abstract void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view);

    public abstract void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view);

    @Override
    public final void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(false);

        final InstancesScopeView view = getInstancesScopeView(e);
        if (view != null && e.getProject() != null) {
            update(e, view);
        }
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        final InstancesScopeView view = getInstancesScopeView(e);
        if (view != null && e.getProject() != null) {
            actionPerformed(e, view);
        }
    }

    protected static InstancesScopeView getInstancesScopeView(AnActionEvent e) {
        return InstancesScopeView.INSTANCES_VIEW_DATA_KEY.getData(e.getDataContext());
    }
}
