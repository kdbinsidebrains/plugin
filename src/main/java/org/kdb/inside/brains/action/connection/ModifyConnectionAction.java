package org.kdb.inside.brains.action.connection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

public class ModifyConnectionAction extends EdtAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Presentation presentation = e.getPresentation();
        if (project == null) {
            presentation.setEnabled(false);
        } else {
            presentation.setEnabled(KdbConnectionManager.getManager(project).getActiveConnection() != null);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final InstanceConnection activeConnection = KdbConnectionManager.getManager(project).getActiveConnection();
        if (activeConnection == null) {
            return;
        }

        final KdbInstance instance = activeConnection.getInstance();
        final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.UPDATE, project, instance);
        if (editor.showAndGet()) {
            instance.updateFrom(editor.createInstance());
        }
    }
}
