package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.StructuralItem;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import java.util.Set;
import java.util.stream.Collectors;

public class CloneItemAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        e.getPresentation().setEnabled(item instanceof KdbInstance);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        if (e.getProject() == null) {
            return;
        }

        if (!(item instanceof KdbInstance)) {
            return;
        }

        final KdbInstance instance = (KdbInstance) item;
        final Set<String> busyNames = instance.getParent().getChildren().stream().map(InstanceItem::getName).collect(Collectors.toSet());
        final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.CREATE, e.getProject(), instance, s -> !s.isBlank() && !busyNames.contains(s));
        if (editor.showAndGet()) {
            final KdbInstance updated = editor.createInstance();

            final StructuralItem folder = instance.getParent();
            final int i = folder.childIndex(instance);

            final KdbInstance newInstance = folder.createInstance(updated.getName(), updated.getHost(), updated.getPort(), updated.getCredentials(), updated.getOptions());
            folder.moveItem(newInstance, i + 1);

            getInstancesScopeView(e).selectItem(newInstance);
        }
    }
}
