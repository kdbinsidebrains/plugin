package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

public class FindInPathAction extends AbstractInstancesAction {
    @Override
    public void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        view.showSearchAndReplace(false);
    }
}
