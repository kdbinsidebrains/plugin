package org.kdb.inside.brains.view.treeview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import icons.KdbIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.treeview.actions.ExportScopesAction;
import org.kdb.inside.brains.view.treeview.actions.ImportScopesAction;
import org.kdb.inside.brains.view.treeview.scope.ScopesEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@State(name = "KdbInstancesView", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class InstancesToolWindow implements Disposable, PersistentStateComponent<Element>, DumbAware {
    private final Project project;

    private String uninitializedActiveScope;
    private ContentManager contentManager;

    private final KdbScopesManager scopesManager;
    private final KdbScopesListener scopesListener = new TheKdbScopesListener();

    private final KdbConnectionManager connectionManager;
    private final KdbConnectionListener connectionListener = new TheKdbConnectionListener();

    private final Map<String, Element> uninitializedViewState = new HashMap<>();

    public InstancesToolWindow(Project project) {
        this.project = project;

        scopesManager = KdbScopesManager.getManager(project);
        scopesManager.addScopesListener(scopesListener);

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(connectionListener);
    }

    public void initToolWindow(@NotNull ToolWindowEx toolWindow) {
        this.contentManager = toolWindow.getContentManager();

        final DumbAwareAction manageScopes = new DumbAwareAction("Manage KDB Scopes", "Manage KDB instance views", KdbIcons.Scope.Icon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final KdbScope scopeToSelect = getActiveScope();

                final KdbScope scope = ScopesEditorDialog.showDialog(project, scopeToSelect);
                final Content content = findContent(s -> s == scope);
                if (content != null) {
                    contentManager.setSelectedContent(content);
                }
            }
        };

        toolWindow.setTitleActions(List.of(manageScopes));

        final ExecutionOptions executionOptions = KdbSettingsService.getInstance().getExecutionOptions();

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(manageScopes);
        group.addSeparator();
        group.add(new DumbAwareToggleAction("Select Activate Instance", "Select active instance based on the instances toolbar", null) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return executionOptions.isSelectActiveConnectionInTree();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                executionOptions.setSelectActiveConnectionInTree(state);
            }
        });
        group.addSeparator();
        group.add(new ExportScopesAction("Export Scopes", "Export all scopes into xml file", scopesManager::getScopes));
        group.add(new ImportScopesAction("Import Scopes", "Import scopes from xml file", scopesManager::addScope, scopesManager::getNames));

        toolWindow.setAdditionalGearActions(group);

        final List<KdbScope> scopes = scopesManager.getScopes();
        if (scopes.isEmpty()) {
            createDummyPage();
        } else {
            scopes.forEach(this::createScope);
            activateScope(uninitializedActiveScope);
        }
    }

    private void activeConnectionChanged(InstanceConnection connection) {
        if (!KdbSettingsService.getInstance().getExecutionOptions().isSelectActiveConnectionInTree()) {
            return;
        }

        getInstancesScopeViews().forEach(v -> v.selectItem(null));
        if (connection == null) {
            return;

        }

        final KdbInstance instance = connection.getInstance();
        final KdbScope scope = instance.getScope();
        if (scope == null) {
            return;
        }

        final InstancesScopeView view = activateScope(scope.getName());
        if (view != null) {
            view.selectItem(instance);
        }
    }

    private void createScope(KdbScope scope) {
        final InstancesScopeView panel = new InstancesScopeView(project, scope, connectionManager);

        final Element state = uninitializedViewState.remove(scope.getName());
        if (state != null) {
            panel.readExternal(state);
        }

        final Content content = UIUtils.createContent(panel, scope.getName(), true);
        content.setPreferredFocusableComponent(panel.getPreferredFocusableComponent());

        final Content cnt = contentManager.getContent(0);
        if (contentManager.getContentCount() == 1 && cnt != null && cnt.getComponent() instanceof DummyPage) {
            contentManager.removeAllContents(true);
        }
        contentManager.addContent(content);

        Disposer.register(this, content);
    }

    private void removeScope(KdbScope scope) {
        final Content content = findContent(s -> s == scope);
        if (content != null) {
            contentManager.removeContent(content, true);
            if (contentManager.getContentCount() == 0) {
                createDummyPage();
            }
        }
    }

    private void createDummyPage() {
        final Content content = UIUtils.createContent(new DummyPage(project), "KDB Instances", false);
        contentManager.addContent(content);
    }

    private KdbScope getActiveScope() {
        final InstancesScopeView activeView = getActiveView();
        return activeView == null ? null : activeView.getScope();
    }

    private InstancesScopeView getActiveView() {
        final Content selectedContent = contentManager.getSelectedContent();
        return selectedContent != null && selectedContent.getComponent() instanceof InstancesScopeView ? ((InstancesScopeView) selectedContent.getComponent()) : null;
    }

    private List<InstancesScopeView> getInstancesScopeViews() {
        if (contentManager == null) {
            return List.of();
        }
        return Stream.of(contentManager.getContents()).map(Content::getComponent).filter(c -> c instanceof InstancesScopeView).map(c -> (InstancesScopeView) c).collect(Collectors.toList());
    }

    @Nullable
    private Content findContent(Predicate<KdbScope> scopeTester) {
        if (contentManager == null) {
            return null;
        }

        for (final Content content : contentManager.getContents()) {
            final InstancesScopeView scopeView = getScopeView(content);
            if (scopeView != null && scopeTester.test(scopeView.getScope())) {
                return content;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Element getState() {
        final Element parentNode = new Element("KdbInstancesView");

        final KdbScope activeScope = getActiveScope();
        if (activeScope != null) {
            parentNode.setAttribute("active-scope", activeScope.getName());
        }

        final Element views = new Element("scope-views");
        for (InstancesScopeView scopeView : getInstancesScopeViews()) {
            final Element el = new Element("scope-view");
            el.setAttribute("name", scopeView.getScope().getName());
            try {
                scopeView.writeExternal(el);
            } catch (WriteExternalException ignore) {
                continue;
            }
            views.addContent(el);
        }

        for (Element element : uninitializedViewState.values()) {
            views.addContent(element.clone());
        }

        parentNode.addContent(views);

        return parentNode;
    }

    @Override
    public void loadState(@NotNull Element state) {
        final String activeScope = state.getAttributeValue("active-scope");

        if (contentManager == null) {
            uninitializedActiveScope = activeScope;
        } else {
            activateScope(activeScope);
        }

        final Element views = state.getChild("scope-views");
        if (views != null) {
            for (Element child : views.getChildren()) {
                final String name = child.getAttributeValue("name");
                if (name == null) {
                    continue;
                }
                final Content content = findContent(s -> s.getName().equals(name));
                final InstancesScopeView scopeView = getScopeView(content);
                if (scopeView != null) {
                    scopeView.readExternal(child);
                } else {
                    uninitializedViewState.put(name, child);
                }
            }
        }
    }

    @Override
    public void dispose() {
        scopesManager.removeScopesListener(scopesListener);
        connectionManager.removeConnectionListener(connectionListener);
    }

    private InstancesScopeView activateScope(String activeScope) {
        final Content content = findContent(s -> s.getName().equals(activeScope));
        if (content != null) {
            contentManager.setSelectedContent(content, false, false);
            return (InstancesScopeView) content.getComponent();
        }
        return null;
    }

    private InstancesScopeView getScopeView(Content content) {
        if (content == null) {
            return null;
        }
        final JComponent component = content.getComponent();
        if (component instanceof InstancesScopeView) {
            return (InstancesScopeView) component;
        }
        return null;
    }

    private class TheKdbConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
            activeConnectionChanged(activated);
        }
    }

    private class TheKdbScopesListener implements KdbScopesListener {
        @Override
        public void scopeCreated(KdbScope scope) {
            createScope(scope);
        }

        @Override
        public void scopeRemoved(KdbScope scope) {
            removeScope(scope);
        }

        @Override
        public void scopeUpdated(KdbScope scope) {
            final Content content = findContent(s -> s == scope);
            if (content == null) {
                return;
            }
            content.setTabName(scope.getName());
            content.setDisplayName(scope.getName());
        }

        @Override
        public void scopesReordered(List<String> oldSort, List<KdbScope> scopes) {
            int index = -1;
            final Content selectedContent = contentManager.getSelectedContent();
            for (KdbScope scope : scopes) {
                index++;
                final Content content = findContent(s -> s == scope);
                if (content == null) {
                    continue;
                }

                contentManager.removeContent(content, false);
                contentManager.addContent(content, index);
            }
            if (selectedContent != null) {
                contentManager.setSelectedContent(selectedContent);
            }
        }
    }

    private static class DummyPage extends JPanel {
        public DummyPage(final Project project) {
            super(new GridBagLayout());
            setBackground(JBColor.background());

            final var activeLabel = new JLabel("<html><center><a href=\"mock\">Create New Scope</a></html>");
            activeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            activeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ScopesEditorDialog.showDialog(project, null);
                }
            });
            final var infoLabel = new JLabel("<html><center>to be able to manage Kdb Instances.</center></html>");

            final JPanel labelsPanel = new JPanel(new GridLayoutManager(2, 1));
            labelsPanel.setBackground(JBColor.background());

            final GridConstraints constrains = new GridConstraints();
            constrains.setRow(0);
            labelsPanel.add(activeLabel, constrains);
            constrains.setRow(1);
            labelsPanel.add(infoLabel, constrains);

            add(labelsPanel, new GridBagConstraints());
        }
    }
}
