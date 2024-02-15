package org.kdb.inside.brains.action.connection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.Project;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionManager;

public class ToggleConnectAction extends EdtAction implements Toggleable {
    private final InstanceConnection connection;

    public ToggleConnectAction() {
        this(null);
    }

    public ToggleConnectAction(InstanceConnection connection) {
        this.connection = connection;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();

        final InstanceConnection connection = getInstanceConnection(e);
        if (connection == null) {
            presentation.setIcon(KdbIcons.Instance.Disconnected);
            presentation.setText("No Instance Selected");
            presentation.setEnabled(false);
            Toggleable.setSelected(presentation, false);
        } else {
            final InstanceState state = connection.getState();
            if (state.isDisconnectable()) {
                presentation.setIcon(KdbIcons.Instance.Connected);
                presentation.setText("Disconnect the Instance");
                presentation.setEnabled(true);
                Toggleable.setSelected(presentation, true);
            } else if (state.isConnectable()) {
                presentation.setIcon(KdbIcons.Instance.Disconnected);
                presentation.setText("Connect the Instance");
                presentation.setEnabled(true);
                Toggleable.setSelected(presentation, false);
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final InstanceConnection connection = getInstanceConnection(e);
        if (connection == null) {
            return;
        }

        final InstanceState state = connection.getState();
        if (state.isDisconnectable()) {
            connection.disconnect();
        } else if (state.isConnectable()) {
            connection.connect();
        }
    }

    @Nullable
    private InstanceConnection getInstanceConnection(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return null;
        }
        return getActiveConnection(project, e);
    }

    protected InstanceConnection getActiveConnection(@NotNull Project project, @NotNull AnActionEvent e) {
        if (connection != null) {
            return connection;
        }
        return KdbConnectionManager.getManager(project).getActiveConnection();
    }
}
