package org.kdb.inside.brains.action.connection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.KdbToolWindowManager;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import java.util.List;

public class CreateConnectionAction extends BgtAction {
    public CreateConnectionAction() {
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(KdbToolWindowManager.isPluginEnabled(e.getProject()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext context = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(context);
        if (project == null) {
            return;
        }
        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);

        final InstanceConnection activeConnection = manager.getActiveConnection();
        final KdbInstance template = activeConnection != null ? manager.getActiveConnection().getInstance() : null;

        final KdbScope scope = template != null ? template.getScope() : null;

        final List<KdbScope> scopes = KdbScopesManager.getManager(project).getScopes();
        if (scope != null || scopes.isEmpty()) {
            showEditor(project, manager, template, scope);
        } else {
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
            JBPopupFactory.getInstance().createActionGroupPopup("Root Kdb Scope", g, context, JBPopupFactory.ActionSelectionAid.NUMBERING, false).showCenteredInCurrentWindow(project);
        }
    }

    private void showEditor(Project project, KdbConnectionManager manager, KdbInstance template, KdbScope scope) {
        final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.FAKE, project, scope, template);
        if (editor.showAndGet()) {
            manager.activate(manager.createTempInstance(editor.createInstance(), scope));
        }
    }
}
