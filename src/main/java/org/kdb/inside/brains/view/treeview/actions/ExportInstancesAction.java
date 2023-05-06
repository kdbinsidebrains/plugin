package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.PackageItem;
import org.kdb.inside.brains.core.ScopeType;

import java.util.List;

public class ExportInstancesAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        final Presentation presentation = e.getPresentation();

        presentation.setEnabledAndVisible(item != null);

        if (item instanceof KdbScope) {
            presentation.setText("Export Scope");
            presentation.setDescription("Export the scope into separate a file");
        } else if (item instanceof PackageItem) {
            presentation.setText("Export Package");
            presentation.setDescription("Export the package and all subtree into separate a file");
        } else {
            presentation.setText("Export Instance");
            presentation.setDescription("Export the instance into separate a file");
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        KdbScope scope = item.getScope();

        if (!(item instanceof KdbScope)) {
            scope = new KdbScope(item.getName() + " Items", ScopeType.LOCAL);
            scope.copyItem(item);
        }

        final List<KdbScope> scopes = List.of(scope);
        new ExportScopesAction("", "", () -> scopes).actionPerformed(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
