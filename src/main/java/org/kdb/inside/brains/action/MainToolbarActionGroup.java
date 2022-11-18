package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.facet.KdbFacetStateManager;

public class MainToolbarActionGroup extends DefaultActionGroup {
    public MainToolbarActionGroup() {
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(KdbFacetStateManager.isFacetEnabled());
    }
}
