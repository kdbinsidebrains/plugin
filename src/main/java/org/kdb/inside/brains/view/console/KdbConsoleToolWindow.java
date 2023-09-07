package org.kdb.inside.brains.view.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.settings.KdbSettingsService;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(name = "KdbConsole", storages = {@Storage("kdb-console.xml")})
public class KdbConsoleToolWindow implements PersistentStateComponent<Element>, Disposable, DumbAware {
    private ContentManager contentManager;

    private final Project project;
    private final KdbConnectionManager connectionManager;
    private final Map<KdbInstance, Element> statesCache = new HashMap<>();

    private final TheConnectionListener listener = new TheConnectionListener();

    private static final @NotNull Icon EXECUTE_ICON = AllIcons.Actions.Execute;
    private static final @NotNull Icon EXECUTED_ICON = AllIcons.Toolwindows.Notifications;

    public KdbConsoleToolWindow(Project project) {
        this.project = project;

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addQueryListener(listener);
        connectionManager.addConnectionListener(listener);
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        contentManager = toolWindow.getContentManager();
        contentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                final Content content = event.getContent();
                final InstanceConnection connection = getConnection(content);
                if (connection == null) {
                    return;
                }

                if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                    content.setIcon(null);
                    if (connectionManager.getActiveConnection() != connection) {
                        connectionManager.activate(connection.getInstance());
                    }
                } else {
                    if (connection.getQuery() != null) {
                        content.setIcon(EXECUTE_ICON);
                    }
                }
            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                final Content content = event.getContent();
                final InstanceConnection connection = getConnection(content);
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });

        toolWindow.setAutoHide(false);
        toolWindow.setShowStripeButton(true);
        toolWindow.setToHideOnEmptyContent(false);
        toolWindow.setTabActions(ActionManager.getInstance().getAction("Kdb.NewConnection"));

        restoreState();
    }

    private void restoreState() {
        for (Map.Entry<KdbInstance, Element> entry : statesCache.entrySet()) {
            final InstanceConnection connection = connectionManager.register(entry.getKey());

            final Content content = getOrcreateInstanceContent(connection);
            if (content.getComponent() instanceof KdbConsolePanel console) {
                console.loadState(entry.getValue());
            }
        }

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
        final Content content = getOrcreateInstanceContent(connection);
        contentManager.setSelectedContent(content, false, false, false);
        return content;
    }

    @NotNull
    private Content getOrcreateInstanceContent(InstanceConnection connection) {
        final Content c = findContent(connection);
        if (c != null) {
            return c;
        }

        final ConsoleSplitType splitType = KdbSettingsService.getInstance().getConsoleOptions().getSplitType();

        final KdbConsolePanel panel = new KdbConsolePanel(project, connection, splitType, p -> {
            final Content content = findContent(p.getConnection());
            if (content != null) {
                contentManager.removeContent(content, true);
            }
        });
        Disposer.register(this, panel);

        final Content content = UIUtils.createContent(panel, connection.getCanonicalName(), true);
        content.setPinnable(true);
        content.setCloseable(true);
        content.setComponent(panel);
        content.setShouldDisposeContent(true);
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);

        contentManager.addContent(content);

        return content;
    }

    public static KdbConsoleToolWindow getInstance(Project project) {
        return project.getService(KdbConsoleToolWindow.class);
    }

    private Content findContent(InstanceConnection connection) {
        final Content[] contents = contentManager.getContents();
        for (Content content : contents) {
            final InstanceConnection c = getConnection(content);
            if (connection.equals(c)) {
                return content;
            }
        }
        return null;
    }

    private InstanceConnection getConnection(Content content) {
        final JComponent component = content.getComponent();
        if (component instanceof KdbConsolePanel) {
            return ((KdbConsolePanel) component).getConnection();
        }
        return null;
    }

    @Override
    public @Nullable Element getState() {
        if (contentManager == null) {
            return null;
        }

        final Element c = new Element("consoles");

        statesCache.clear();
        final Content[] contents = contentManager.getContents();
        for (Content content : contents) {
            if (content.getComponent() instanceof KdbConsolePanel console) {
                final InstanceConnection connection = console.getConnection();
                if (connection.isTemporal()) {
                    continue;
                }

                final Element e = new Element("console");
                e.setAttribute("scope", connection.getInstance().getScope().getName());
                e.setAttribute("instance", connection.getCanonicalName());
                e.setAttribute("connected", String.valueOf(connection.getState() == InstanceState.CONNECTED));
                console.saveState(e);
                statesCache.put(connection.getInstance(), e);

                c.addContent(e);
            }
        }
        return c;
    }

    @Override
    public void loadState(@NotNull Element state) {
        final KdbScopesManager scopesManager = KdbScopesManager.getManager(project);

        statesCache.clear();
        final List<Element> children = state.getChildren();
        for (Element child : children) {
            final String scopeName = child.getAttributeValue("scope");
            if (scopeName == null) {
                continue;
            }
            final KdbScope scope = scopesManager.getScope(scopeName);
            if (scope == null) {
                continue;
            }

            final String instanceName = child.getAttributeValue("instance");
            if (instanceName == null) {
                continue;
            }
            final KdbInstance instance = scopesManager.lookupInstance(instanceName);
            if (instance == null) {
                continue;
            }
            statesCache.put(instance, child);

            final InstanceConnection connection = connectionManager.register(instance);
            if (Boolean.parseBoolean(child.getAttributeValue("connected", "false"))) {
                connection.connect();
            }
        }
    }

    @Override
    public void dispose() {
        statesCache.clear();
        connectionManager.removeQueryListener(listener);
        connectionManager.removeConnectionListener(listener);
    }

    private class TheConnectionListener implements KdbConnectionListener, KdbQueryListener {
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

        @Override
        public void queryStarted(InstanceConnection connection, KdbQuery query) {
            final Content content = findContent(connection);
            if (content != null && contentManager.getSelectedContent() != content) {
                content.setIcon(EXECUTE_ICON);
            }
        }

        @Override
        public void queryFinished(InstanceConnection connection, KdbQuery query, KdbResult result) {
            final Content content = findContent(connection);
            if (content != null && contentManager.getSelectedContent() != content) {
                content.setIcon(EXECUTED_ICON);
            }
        }
    }
}