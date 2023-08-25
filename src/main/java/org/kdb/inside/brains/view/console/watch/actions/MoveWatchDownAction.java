package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.WatchesView;

import java.util.List;

public class MoveWatchDownAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        watchesView.moveWatchDown(watchesView.getSelectedVariables());
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> nodes = watchesView.getSelectedVariables();
        final List<VariableNode> allVariables = watchesView.getAllVariables();
        for (VariableNode node : nodes) {
            if (allVariables.indexOf(node) < allVariables.size() - 1) {
                return true;
            }
        }
        return false;
    }
}
