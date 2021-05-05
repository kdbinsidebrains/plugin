package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbConnectionManager;

import java.util.List;

public class ExecuteOnAction extends ActionGroup implements DumbAware {
    public ExecuteOnAction() {
    }

    public ExecuteOnAction(@NlsActions.ActionText String shortName, boolean popup) {
        super(shortName, popup);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }

        final Project project = e.getProject();
        if (project == null) {
            return EMPTY_ARRAY;
        }

        final KdbConnectionManager instance = KdbConnectionManager.getManager(project);
        final List<InstanceConnection> connections = instance.getConnections();

        int i = 0;
        final AnAction[] res = new AnAction[connections.size()];
        for (InstanceConnection kdbInstance : connections) {
            res[i++] = new ExecuteAction(kdbInstance);
        }
        return res;
    }
}