package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.WatchesView;

import java.util.List;

public class ExpandWatchesAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> nodes = watchesView.getSelectedVariables();
        if (nodes != null) {
            watchesView.expandNodes(nodes);
        }
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> nodes = watchesView.getSelectedVariables();
        return nodes != null && nodes.stream().anyMatch(watchesView::isExpandable);
    }
}
