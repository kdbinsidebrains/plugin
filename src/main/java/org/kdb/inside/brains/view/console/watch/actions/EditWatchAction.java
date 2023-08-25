package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.WatchesView;

import java.util.List;

public class EditWatchAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> selectedNodes = watchesView.getSelectedVariables();
        if (selectedNodes.size() != 1) {
            return;
        }

        watchesView.editVariable(selectedNodes.get(0));
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        return watchesView.getSelectedVariables().size() == 1;
    }
}
