package org.kdb.inside.brains.view.console;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionListener;
import org.kdb.inside.brains.core.KdbConnectionManager;

import javax.swing.*;
import java.util.List;

public class KdbConsoleToolWindow implements Disposable {
    private ToolWindowEx toolWindow;

    private final Project project;
    private final ContentFactory contentFactory;
    private final KdbConnectionManager connectionManager;

    private final TheConnectionListener listener = new TheConnectionListener();

    public KdbConsoleToolWindow(Project project) {
        this.project = project;

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(listener);

        contentFactory = ContentFactory.SERVICE.getInstance();
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        this.toolWindow = toolWindow;

        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setShowStripeButton(true);
        toolWindow.setTabActions(ActionManager.getInstance().getAction("Kdb.NewConnection"));

        final List<InstanceConnection> connections = connectionManager.getConnections();
        for (InstanceConnection connection : connections) {
            if (connection.getState() == InstanceState.CONNECTED) {
                activateInstance(connection);
            }
        }
    }

    public void execute(InstanceConnection connection, String text) {
        final ToolWindow kdb_console = ToolWindowManager.getInstance(project).getToolWindow("KDB Console");
        if (kdb_console != null) {
            kdb_console.show();
        }

        final Content content = activateInstance(connection);

        final KdbConsolePanel panel = (KdbConsolePanel) content.getComponent();
        panel.execute(text);
    }

    private Content activateInstance(InstanceConnection connection) {
        Content content = findContent(connection);
        final ContentManager manager = toolWindow.getContentManager();

        if (content == null) {
            content = createInstanceContent(connection);
            manager.addContent(content);
        }

        manager.setSelectedContent(content, false, false, false);

        return content;
    }

    @NotNull
    private Content createInstanceContent(InstanceConnection connection) {
        final KdbConsolePanel panel = new KdbConsolePanel(project, connection, p -> {
            final Content content = findContent(p.getConnection());
            if (content != null) {
                toolWindow.getContentManager().removeContent(content, true);
            }
        });

        final Content content = contentFactory.createContent(panel, connection.getCanonicalName(), true);
        content.setPinnable(true);
        content.setCloseable(true);
        content.setComponent(panel);

        return content;
    }

    @Override
    public void dispose() {
        connectionManager.removeConnectionListener(listener);
    }

    public static KdbConsoleToolWindow getInstance(Project project) {
        return project.getService(KdbConsoleToolWindow.class);
    }

    private Content findContent(InstanceConnection connection) {
        final Content[] contents = toolWindow.getContentManager().getContents();
        for (Content content : contents) {
            final JComponent component = content.getComponent();
            if (component instanceof KdbConsolePanel) {
                KdbConsolePanel kdbConsolePanel = (KdbConsolePanel) component;
                if (kdbConsolePanel.getConnection().equals(connection)) {
                    return content;
                }
            }
        }
        return null;
    }

    private class TheConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionCreated(InstanceConnection connection) {
            // TODO: add here?
        }

        @Override
        public void connectionRemoved(InstanceConnection connection) {
            // TODO: remove here?
        }

        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
            if (activated != null) {
                activateInstance(activated);
            }
        }

        @Override
        public void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
            if (newState == InstanceState.CONNECTED) {
                activateInstance(connection);
            }
        }
    }
}