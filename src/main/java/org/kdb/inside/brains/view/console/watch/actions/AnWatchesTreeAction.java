package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.view.console.watch.WatchesView;

public abstract class AnWatchesTreeAction extends BgtAction {
    protected void update(@NotNull final AnActionEvent e, @Nullable WatchesView watchesView) {
    }

    protected abstract void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView);

    protected abstract boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView);

    @Override
    public void update(@NotNull final AnActionEvent e) {
        final WatchesView watchesView = e.getData(WatchesView.DATA_KEY);
        e.getPresentation().setEnabled(watchesView != null && isEnabled(e, watchesView));
        update(e, watchesView);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final WatchesView watchesView = e.getData(WatchesView.DATA_KEY);
        if (watchesView != null) {
            perform(e, watchesView);
        }
    }
}