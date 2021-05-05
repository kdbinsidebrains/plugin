package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.StructuralItem;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

public class NewInstanceAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        final Presentation presentation = e.getPresentation();
        final boolean enabled = item instanceof StructuralItem;
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            presentation.setEnabledAndVisible(enabled);
        } else {
            presentation.setEnabled(enabled);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        if (e.getProject() == null) {
            return;
        }

        if (!(item instanceof StructuralItem)) {
            return;
        }

        final StructuralItem folder = (StructuralItem) item;
        final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.CREATE, e.getProject(), folder, null);
        if (editor.showAndGet()) {
            final KdbInstance updated = editor.createInstance();

            final KdbInstance instance = folder.createInstance(updated.getName(), updated.getHost(), updated.getPort(), updated.getCredentials(), updated.getOptions());

            getInstancesScopeView(e).selectItem(instance);
        }
    }
}
