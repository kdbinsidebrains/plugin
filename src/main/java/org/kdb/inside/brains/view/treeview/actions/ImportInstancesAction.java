package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbScopesManager;
import org.kdb.inside.brains.core.StructuralItem;

public class ImportInstancesAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        e.getPresentation().setEnabled(item instanceof StructuralItem);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }

        final StructuralItem structuralItem = (StructuralItem) item;
        final KdbScopesManager manager = KdbScopesManager.getManager(project);
        new ImportScopesAction("", "", scope -> {
            for (InstanceItem instanceItem : scope) {
                structuralItem.copyItem(instanceItem);
            }
        }, manager::getNames).performImport(e);
    }
}
