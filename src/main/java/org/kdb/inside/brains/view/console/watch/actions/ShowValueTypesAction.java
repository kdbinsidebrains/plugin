package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.watch.WatchesView;

public class ShowValueTypesAction extends AnWatchesTreeAction implements Toggleable {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final boolean state = !watchesView.isShowTypes();
        watchesView.setShowTypes(state);
        Toggleable.setSelected(e.getPresentation(), state);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, WatchesView watchesView) {
        Toggleable.setSelected(e.getPresentation(), watchesView != null && watchesView.isShowTypes());
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        return true;
    }
}
