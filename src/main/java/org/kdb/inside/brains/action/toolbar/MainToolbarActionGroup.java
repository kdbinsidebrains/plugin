package org.kdb.inside.brains.action.toolbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbToolWindowManager;

public class MainToolbarActionGroup extends DefaultActionGroup {
    public MainToolbarActionGroup() {
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(KdbToolWindowManager.isPluginEnabled(e.getProject()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
