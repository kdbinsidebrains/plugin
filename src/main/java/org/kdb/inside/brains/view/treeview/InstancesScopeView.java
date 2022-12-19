package org.kdb.inside.brains.view.treeview;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import icons.KdbIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.view.treeview.tree.InstancesTree;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstancesScopeView extends SimpleToolWindowPanel implements Disposable, DataProvider, DumbAware {  // implements DataProvider, DockContainer
    private final KdbScope scope;

    private final InstancesTree tree;

    private TreeState readTreeState;
    private final AtomicBoolean treeStateRestored = new AtomicBoolean();

    public static final DataKey<InstancesScopeView> INSTANCES_VIEW_DATA_KEY = DataKey.create("Kdb.Instances.View");

    public InstancesScopeView(Project project, KdbScope scope) {
        super(true);

        this.scope = scope;
        this.tree = new InstancesTree(scope, KdbConnectionManager.getManager(project));

        setToolbar(createToolbar());
        setContent(new JBScrollPane(tree));

        setBackground(UIUtil.SIDE_PANEL_BACKGROUND);


/*
        final AnActionButton addActionButton = AnActionButton.fromAction(ActionManager.getInstance().getAction("AddNewFavoritesList"));
        addActionButton.getTemplatePresentation().setIcon(CommonActionsPanel.Buttons.ADD.getIcon());
        addActionButton.setShortcut(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD));

        final AnActionButton editActionButton = AnActionButton.fromAction(ActionManager.getInstance().getAction("EditFavorites"));
        editActionButton.setShortcut(CommonShortcuts.CTRL_ENTER);

        final AnActionButton deleteActionButton = new DeleteFromFavoritesAction();
        deleteActionButton.setShortcut(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"));
*/

        /*
         */
        //.addExtraAction(exportActionButton);

/*
        final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT);
        action.registerCustomShortcutSet(action.getShortcutSet(), tree);
*/
//        final JPanel panel = decorator.createPanel();

//        panel.setBorder(JBUI.Borders.empty());
//        add(panel, BorderLayout.CENTER);
//        setBorder(JBUI.Borders.empty());
/*

        autoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return false;
                //myFavoritesManager.getViewSettings().isAutoScrollToSource();
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
//                myFavoritesManager.getViewSettings().setAutoScrollToSource(state);
            }
        };
        autoScrollToSourceHandler.install(tree);
*/
    }

    public KdbScope getScope() {
        return scope;
    }

    private JComponent createToolbar() {
        final ActionManager am = ActionManager.getInstance();

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(am.getAction("Kdb.Instances.NewPackage"));
        group.add(am.getAction("Kdb.Instances.NewInstance"));
        group.addSeparator();
        group.add(am.getAction("Kdb.Instances.ModifyItem"));
        group.addSeparator();
        group.add(am.getAction("Kdb.Instances.Connect"));
        group.add(am.getAction("Kdb.Instances.Disconnect"));
        group.addSeparator();
        group.add(am.getAction("Kdb.Instances.MoveItemUp"));
        group.add(am.getAction("Kdb.Instances.MoveItemDown"));
        group.addSeparator();
        group.add(new ToggleAction("Show Connection Details", "Show connection details with the node name", KdbIcons.Node.ShowConnectionFilter) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return tree.isShownConnectionDetails();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                tree.showConnectionDetails(state);
                tree.repaint();
            }
        });
        group.addSeparator();
        group.add(CommonActionsManager.getInstance().createExpandAllHeaderAction(tree));
        group.add(CommonActionsManager.getInstance().createCollapseAllHeaderAction(tree));

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.INSTANCES_VIEW_TOOLBAR, group, true);
        actionToolbar.setTargetComponent(this);
        return actionToolbar.getComponent();
    }

    public JComponent getPreferredFocusableComponent() {
        return tree;
    }

    public void selectItem(InstanceItem item) {
        tree.selectItem(item);
    }

    public void selectItems(List<InstanceItem> items) {
        tree.selectItems(items);
    }

    public List<InstanceItem> getSelectedItems() {
        return tree.getSelectedItems();
    }

    @Override
    public void dispose() {
        Disposer.dispose(tree);
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (INSTANCES_VIEW_DATA_KEY.is(dataId)) {
            return this;
        }

        if (PlatformDataKeys.CUT_PROVIDER.is(dataId)) {
            return tree;
        }
        if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
            return tree;
        }
        if (PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
            return tree;
        }
        if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
            return tree;
        }

        return super.getData(dataId);
    }

    public void readExternal(@NotNull Element element) {
        final TreeState treeState = TreeState.createFrom(element);
        if (!treeState.isEmpty()) {
            readTreeState = treeState;
        }
        tree.showConnectionDetails(Boolean.parseBoolean(element.getAttributeValue("ShowConnectionDetails", "true")));
        restoreExpandedPaths();
    }

    public void writeExternal(Element element) {
        element.setAttribute("ShowConnectionDetails", String.valueOf(tree.isShownConnectionDetails()));

        saveExpandedPaths();

        if (readTreeState != null) {
            readTreeState.writeExternal(element);
        }
    }

    private void saveExpandedPaths() {
        treeStateRestored.set(false);
        if (tree != null) {
            TreeState treeState = TreeState.createOn(tree);
            if (!treeState.isEmpty()) {
                readTreeState = treeState;
            } else {
                readTreeState = null;
            }
        }
    }

    private void restoreExpandedPaths() {
        if (tree == null || treeStateRestored.getAndSet(true)) {
            return;
        }
        if (readTreeState != null && !readTreeState.isEmpty()) {
            readTreeState.applyTo(tree);
        } else if (tree.isSelectionEmpty()) {
            TreeUtil.promiseSelectFirst(tree);
        }
    }
}
