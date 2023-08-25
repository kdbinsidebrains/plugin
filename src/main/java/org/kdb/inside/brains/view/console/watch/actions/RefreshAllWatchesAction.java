package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.watch.WatchesView;

public class RefreshAllWatchesAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        watchesView.refreshAllVariables();
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        return !watchesView.isEmpty();
    }
}
