package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.PackageItem;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;
import org.kdb.inside.brains.view.treeview.scope.ScopesEditorDialog;

public class ModifyItemAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        final Presentation presentation = e.getPresentation();
        if (item instanceof KdbScope) {
            presentation.setText("Modify Scope");
            presentation.setEnabledAndVisible(true);
        } else if (item instanceof PackageItem) {
            presentation.setText("Rename Package");
            presentation.setEnabledAndVisible(true);
        } else if (item instanceof KdbInstance) {
            presentation.setText("Modify Instance");
            presentation.setEnabledAndVisible(true);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }

        if (item instanceof KdbScope) {
            ScopesEditorDialog.showDialog(project, (KdbScope) item);
        } else if (item instanceof PackageItem) {
            NewPackageAction.modifyPackage(e, (PackageItem) item);
        } else if (item instanceof KdbInstance) {
            final KdbInstance instance = (KdbInstance) item;
            final InstanceEditorDialog instanceEditor = new InstanceEditorDialog(InstanceEditorDialog.Mode.UPDATE, project, instance);
            if (instanceEditor.showAndGet()) {
                instance.updateFrom(instanceEditor.createInstance());
            }
        }
    }
}
