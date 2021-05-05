package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import java.awt.*;
import java.util.List;

public class CreateConnectionAction extends AnAction {
    public CreateConnectionAction() {
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext context = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(context);
        if (project == null) {
            return;
        }
        final Component component = e.getInputEvent().getComponent();
        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);

        final InstanceConnection activeConnection = manager.getActiveConnection();
        final KdbInstance template = activeConnection != null ? manager.getActiveConnection().getInstance() : null;

        final KdbScope scope = template != null ? template.getScope() : null;

        final List<KdbScope> scopes = KdbScopesManager.getManager(project).getScopes();
        if (scope == null && !scopes.isEmpty()) {
            DefaultActionGroup g = new DefaultActionGroup();
            g.add(new DumbAwareAction("No Scope") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    showEditor(project, manager, template, null);
                }
            });
            for (KdbScope s : scopes) {
                g.add(new DumbAwareAction(s.getName()) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        showEditor(project, manager, template, s);
                    }
                });
            }
            JBPopupFactory.getInstance().createActionGroupPopup("Root Kdb Scope", g, context, JBPopupFactory.ActionSelectionAid.NUMBERING, false).showUnderneathOf(component);
        } else {
            showEditor(project, manager, template, scope);
        }
    }

    private void showEditor(Project project, KdbConnectionManager manager, KdbInstance template, KdbScope scope) {
        final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.FAKE, project, scope, template);
        if (editor.showAndGet()) {
            manager.activate(manager.createTempInstance(editor.createInstance(), scope));
        }
    }
}
