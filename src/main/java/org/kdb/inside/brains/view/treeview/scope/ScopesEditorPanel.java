package org.kdb.inside.brains.view.treeview.scope;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.CommonActionsPanel;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.treeview.actions.ExportScopesAction;
import org.kdb.inside.brains.view.treeview.actions.ImportScopesAction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScopesEditorPanel extends MasterDetailsComponent {
    private final Project project;
    private final KdbScopesManager scopesManager;

    private static final String STATE_KEY = "KdbInstancesScopeState";

    public ScopesEditorPanel(Project project) {
        super(new ScopesOrdersState());
        this.project = project;
        this.scopesManager = project.getService(KdbScopesManager.class);
        initTree();
    }

    @Override
    public String getDisplayName() {
        return "KDB Instances Scope";
    }

    @Override
    protected String getEmptySelectionString() {
        return "Select a scope to view or edit its details here";
    }

    @Override
    protected String getComponentStateKey() {
        return STATE_KEY;
    }

    @Override
    protected Dimension getPanelPreferredSize() {
        return JBUI.size(400, 200);
    }

    @Override
    protected MasterDetailsStateService getStateService() {
        return MasterDetailsStateService.getInstance(project);
    }

    public KdbScope getSelectedScope() {
        final NamedConfigurable<?> selectedConfigurable = getSelectedConfigurable();
        if (selectedConfigurable instanceof ScopeConfigurable) {
            final ScopeConfigurable configurable = (ScopeConfigurable) selectedConfigurable;
            return configurable.getOriginalScope();
        }
        return null;
    }

    public List<KdbScope> getSelectedScopes() {
        final TreePath[] selectionPaths = myTree.getSelectionPaths();
        if (selectionPaths == null) {
            return List.of();
        }
        return Stream.of(selectionPaths).map(c -> (MyNode) c.getLastPathComponent())
                .map(c -> (ScopeConfigurable) c.getConfigurable())
                .map(ScopeConfigurable::getOriginalScope).collect(Collectors.toList());
    }

    @Override
    protected ArrayList<AnAction> createActions(final boolean fromPopup) {
        final ArrayList<AnAction> result = new ArrayList<>();
        result.add(new MyAddAction(fromPopup));
        result.add(new MyDeleteAction(forAll(o -> {
            if (o instanceof MyNode) {
                final NamedConfigurable<?> namedConfigurable = ((MyNode) o).getConfigurable();
                final Object editableObject = namedConfigurable != null ? namedConfigurable.getEditableObject() : null;
                return editableObject instanceof KdbScope;
            }
            return false;
        })));
        result.add(new MyCopyAction());
        result.add(new MyMoveAction("Move Up", IconUtil.getMoveUpIcon(), -1));
        result.add(new MyMoveAction("Move Down", IconUtil.getMoveDownIcon(), 1));
        result.add(Separator.create());
        result.add(new ExportScopesAction("Export Scopes", "Exports selected scopes to xml", this::getSelectedScopes));
        result.add(new ImportScopesAction("Import Scopes", "Import scopes from a file", this::importScope, this::collectScopeNames));
        return result;
    }

    @Override
    protected void initTree() {
        super.initTree();
        myTree.setShowsRootHandles(false);
        new TreeSpeedSearch(myTree, true, treePath -> ((MyNode) treePath.getLastPathComponent()).getDisplayName());

        final ColoredTreeCellRenderer cellRenderer = (ColoredTreeCellRenderer) myTree.getCellRenderer();

        final Map<KdbScope, String> instancesCount = new LinkedHashMap<>();
        myTree.setCellRenderer((tree, value, selected, expanded, leaf, row, hasFocus) -> {
            cellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof MyNode && ((MyNode) value).getConfigurable() != null) {
                final MyNode node = (MyNode) value;
                final Object editableObject = node.getConfigurable();
                if (!(editableObject instanceof ScopeConfigurable)) {
                    return cellRenderer;
                }

                final ScopeConfigurable scopeConfigurable = (ScopeConfigurable) editableObject;
                final KdbScope scope = scopeConfigurable.getOriginalScope();
                final String note = instancesCount.computeIfAbsent(scope, s -> {
                    final int i = calculateInstancesCount(scope);
                    if (i == 0) {
                        return " (no instances)";
                    } else if (i == 1) {
                        return " (1 instance)";
                    }
                    return " (" + i + " instances)";
                });
                cellRenderer.append(note, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            return cellRenderer;
        });
    }

    private int calculateInstancesCount(StructuralItem scope) {
        int r = 0;
        for (InstanceItem instanceItem : scope) {
            if (instanceItem instanceof KdbInstance) {
                r++;
            } else if (instanceItem instanceof StructuralItem) {
                r += calculateInstancesCount((StructuralItem) instanceItem);
            }
        }
        return r;
    }

    @Override
    public void reset() {
        myRoot.removeAllChildren();

        final List<KdbScope> scopes = scopesManager.getScopes();
        for (KdbScope scope : scopes) {
            addNewScope(scope);
        }

        loadComponentState();

        super.reset();
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();

        final List<KdbScope> newScopes = collectScopeConfigurable().stream().map(ScopeConfigurable::getOriginalScope).collect(Collectors.toList());
        final List<KdbScope> oldScopes = new ArrayList<>(scopesManager.getScopes());
        oldScopes.removeAll(newScopes);
        oldScopes.forEach(scopesManager::removeScope);

        scopesManager.reorderScopes(collectScopeNames());
    }

    @Override
    public boolean isModified() {
        final List<String> strings = collectScopeNames();
        final List<KdbScope> scopes = scopesManager.getScopes();
        if (scopes.size() != strings.size()) {
            return true;
        }

        if (!scopes.stream().map(KdbScope::getName).collect(Collectors.toList()).equals(strings)) {
            return true;
        }

        return super.isModified();
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        if (editableObject instanceof KdbScope) {
            return scopesManager.containsScope((KdbScope) editableObject);
        }
        return false;
    }

    private void createScope(ScopeType type) {
        final String newName = Messages.showInputDialog(myTree, "Name", "Add New Scope", Messages.getInformationIcon(), createUniqueName(type == ScopeType.LOCAL ? "Local Scope" : "Shared Scope"), new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
                return !inputString.isBlank();
            }

            @Override
            public boolean canClose(String inputString) {
                return checkInput(inputString);
            }
        });

        if (newName == null) {
            return;
        }
        addNewScope(new KdbScope(newName, type, InstanceOptions.INHERITED, null));
    }

    private String createUniqueName(String prefix) {
        final List<String> treeScopes = collectScopeNames();
        if (!treeScopes.contains(prefix)) {
            return prefix;
        }

        int i = 1;
        while (true) {
            if (!treeScopes.contains(prefix + i)) {
                return prefix + i;
            }
            i++;
        }
    }

    private List<String> collectScopeNames() {
        return collectScopeConfigurable().stream().map(ScopeConfigurable::getEditableObject).map(InstanceItem::getName).collect(Collectors.toList());
    }

    private List<ScopeConfigurable> collectScopeConfigurable() {
        final int childCount = myRoot.getChildCount();
        final List<ScopeConfigurable> res = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            final MyNode node = (MyNode) myRoot.getChildAt(i);
            res.add((ScopeConfigurable) node.getConfigurable());
        }
        return res;
    }

    private void importScope(KdbScope scope) {
        addNewScope(scope);
    }

    private void addNewScope(final KdbScope scope) {
        final MyNode nodeToAdd = new MyNode(new ScopeConfigurable(scope, scopesManager, TREE_UPDATER));
        myRoot.add(nodeToAdd);
        ((DefaultTreeModel) myTree.getModel()).reload(myRoot);
        selectNodeInTree(nodeToAdd);
    }

    private class MyAddAction extends ActionGroup implements ActionGroupWithPreselection, DumbAware {
        private DumbAwareAction[] myChildren;
        private final boolean fromPopup;

        MyAddAction(boolean fromPopup) {
            super("Add Scope", true);
            this.fromPopup = fromPopup;
            final Presentation presentation = getTemplatePresentation();
            presentation.setIcon(IconUtil.getAddIcon());
            registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD), myTree);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            if (fromPopup) {
                setPopup(false);
            }
        }

        @Override
        public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
            if (myChildren == null) {
                myChildren = new DumbAwareAction[]{new DumbAwareAction("Local", "Local", KdbIcons.Scope.Local) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        createScope(ScopeType.LOCAL);
                    }
                }, new DumbAwareAction("Shared", "Shared", KdbIcons.Scope.Shared) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        createScope(ScopeType.SHARED);
                    }
                }};
            }
            if (fromPopup) {
                final DumbAwareAction action = myChildren[getDefaultIndex()];
                action.getTemplatePresentation().setIcon(IconUtil.getAddIcon());
                return new DumbAwareAction[]{action};
            }
            return myChildren;
        }

        @Override
        public ActionGroup getActionGroup() {
            return this;
        }

        @Override
        public int getDefaultIndex() {
            final TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                final MyNode node = (MyNode) selectionPath.getLastPathComponent();
                Object editableObject = node.getConfigurable().getEditableObject();
                if (editableObject instanceof NamedScope) {
                    editableObject = ((MyNode) node.getParent()).getConfigurable().getEditableObject();
                }
/*
                if (editableObject instanceof NamedScopeManager) {
                    return 0;
                } else if (editableObject instanceof DependencyValidationManager) {
                    return 1;
                }
*/
            }
            return 0;
        }
    }

    private class MyMoveAction extends AnAction {
        private final int myDirection;

        protected MyMoveAction(String text, Icon icon, int direction) {
            super(text, text, icon);
            final ShortcutSet shortcutSet = direction < 0 ? CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.UP) : CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.DOWN);
            registerCustomShortcutSet(shortcutSet, myTree);
            myDirection = direction;
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
            TreeUtil.moveSelectedRow(myTree, myDirection);
        }

        @Override
        public void update(@NotNull final AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            presentation.setEnabled(false);
            final TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (myDirection < 0) {
                    presentation.setEnabled(treeNode.getPreviousSibling() != null);
                } else {
                    presentation.setEnabled(treeNode.getNextSibling() != null);
                }
            }
        }
    }

    private class MyCopyAction extends AnAction {
        MyCopyAction() {
            super("Copy", "Copy", COPY_ICON);
            registerCustomShortcutSet(CommonShortcuts.getDuplicate(), myTree);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            KdbScope scope = (KdbScope) getSelectedObject();
            if (scope != null) {
                final TreePath selectionPath = myTree.getSelectionPath();
                if (selectionPath == null) {
                    return;
                }

                final ScopeConfigurable configurable = (ScopeConfigurable) ((MyNode) selectionPath.getLastPathComponent()).getConfigurable();

                final KdbScope copy = configurable.getEditableObject().copy();
                copy.setName(createUniqueName(copy.getName() + " Copy"));
                addNewScope(copy);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedObject() instanceof KdbScope);
        }
    }

    public static class ScopesOrdersState extends MasterDetailsState {
        @XCollection(propertyElementName = "order", elementName = "scope", valueAttributeName = "name")
        public ArrayList<String> myOrder = new ArrayList<>();
    }
}
