package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.util.NullableFunction;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.treeview.actions.ConnectAction;

import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InstancesTree extends DnDAwareTree implements DataProvider, DnDTargetChecker, DnDDropHandler, CopyProvider, CutProvider, DeleteProvider, PasteProvider, Disposable {
    private final KdbScope scope;
    private final InstancesTreeModel model;
    private final KdbConnectionManager manager;
    private final InstancesTreeRenderer cellRenderer;
    private final InstancesSpeedSearch speedSearch;
    private final InstancesSearchSession searchSession;

    private final TheManagerListener managerListener = new TheManagerListener();

    public InstancesTree(@Nullable Project project, @NotNull KdbScope scope, @NotNull KdbConnectionManager manager) {
        this.manager = manager;

        this.model = new InstancesTreeModel(scope);

        this.searchSession = createSearchSession(project);
        this.speedSearch = new InstancesSpeedSearch(this);

        final DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        this.cellRenderer = new InstancesTreeRenderer(manager, searchSession);

        this.scope = scope;
        this.scope.addScopeListener(model);

        manager.addQueryListener(managerListener);
        manager.addConnectionListener(managerListener);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (speedSearch.isPopupActive()) {
                    speedSearch.hidePopup();
                } else {
                    cancelCutCopy();
                }
            }
        }.registerCustomShortcutSet(KeyEvent.VK_ESCAPE, 0, this);

        final ActionManager am = ActionManager.getInstance();
        am.getAction("Kdb.Instances.SpeedSearchAction").registerCustomShortcutSet(this, this);
        am.getAction("Kdb.Instances.FindInPathAction").registerCustomShortcutSet(this, this);
        am.getAction("Kdb.Instances.ReplaceInPathAction").registerCustomShortcutSet(this, this);

        setOpaque(true);
        setRootVisible(true);
        setShowsRootHandles(true);

        TreeUtil.installActions(this);

        setModel(model);
        setCellRenderer(cellRenderer);
        setSelectionModel(selectionModel);

        PopupHandler.installPopupMenu(this, "Kdb.InstancesScopeView", ActionPlaces.getActionGroupPopupPlace(ActionPlaces.INSTANCES_VIEW_POPUP));

        ToolTipManager.sharedInstance().registerComponent(this);

        enableDnD();

        initListeners();
    }

    @NotNull
    private InstancesSearchSession createSearchSession(@Nullable Project project) {
        return new InstancesSearchSession(project, this);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void enableDnD() {
        DnDSupport.createBuilder(this)
                .setBeanProvider((NullableFunction<DnDActionInfo, DnDDragStartBean>) info -> {
                    final Point point = info.getPoint();
                    if (getPathForLocation(point.x, point.y) == null) {
                        return null;
                    }

                    final List<InstanceItem> selectedItems = getSelectedItems();
                    if (selectedItems.stream().anyMatch(i -> i instanceof KdbScope)) {
                        return null;
                    }
                    return !selectedItems.isEmpty() ? new DnDDragStartBean(selectedItems) : null;
                })
                .enableAsNativeTarget()
                .setDisposableParent(this)
                .setTargetChecker(this)
                .setDropHandler(this)
                .setImageProvider((NullableFunction<DnDActionInfo, DnDImage>) dnDActionInfo -> {
                    Point point = dnDActionInfo.getPoint();
                    TreePath path = getPathForLocation(point.x, point.y);
                    return path == null ? null : new DnDImage(DnDAwareTree.getDragImage(this, path, point).first);
                })
                .install();

    }

    private void initListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ConnectAction.connectInstances(manager, getSelectedItems().stream().filter(p -> p instanceof KdbInstance).map(p -> (KdbInstance) p).collect(Collectors.toList()));
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }

                final TreePath selPath = getPathForLocation(e.getX(), e.getY());
                if (selPath == null) {
                    return;
                }

                final InstanceItem item = TreeUtil.getUserObject(InstanceItem.class, selPath.getLastPathComponent());
                if (item instanceof KdbInstance) {
                    ConnectAction.connectInstances(manager, Collections.singletonList((KdbInstance) item));
                }
            }
        });
    }

    public void expandItem(InstanceItem item) {
        expandPath(item != null ? item.getTreePath() : null);
    }

    public void selectItem(InstanceItem item) {
        final TreePath treePath = item != null ? item.getTreePath() : null;
        expandPath(treePath);
        setSelectionPath(treePath);
        scrollPathToVisible(treePath);
    }

    public void selectItems(List<InstanceItem> selectedItems) {
        final TreePath[] paths = selectedItems.stream().map(InstanceItem::getTreePath).toArray(TreePath[]::new);
        setSelectionPaths(paths);
    }

    public InstanceItem getSelectedItem() {
        final List<InstanceItem> selectedItems = getSelectedItems();
        return selectedItems.isEmpty() ? null : selectedItems.get(0);
    }

    public List<InstanceItem> getSelectedItems() {
        return TreeUtil.collectSelectedObjects(this, treePath -> (InstanceItem) treePath.getLastPathComponent());
    }

    @Override
    public void drop(DnDEvent event) {
        final DnDPosition dnDPosition = getDnDPosition(event);
        if (dnDPosition == null) {
            return;
        }

        final DnDAction action = event.getAction();
        final DnDPlace place = dnDPosition.place;
        final InstanceItem item = dnDPosition.item;

        StructuralItem parent = null;
        if (place == DnDPlace.BEFORE || place == DnDPlace.AFTER) {
            parent = item.getParent();
        } else if (item instanceof StructuralItem) {
            parent = (StructuralItem) item;
        }

        if (parent == null) {
            return;
        }

        int index;
        if (place == DnDPlace.BEFORE) {
            index = parent.childIndex(item);
        } else if (place == DnDPlace.AFTER) {
            index = parent.childIndex(item) + 1;
        } else {
            index = parent.getChildrenCount();
        }

        if (index == -1) {
            return;
        }

        final List<InstanceItem> attachItems = getDropInstance(event);
        if (attachItems.isEmpty()) {
            return;
        }

        int pos = index;
        for (InstanceItem i : attachItems) {
            if (action == DnDAction.COPY) {
                parent.copyItem(i, pos);
            } else {
                parent.moveItem(i, pos);
            }
            pos++;
        }
        expandPath(parent.getTreePath());
        selectItems(attachItems);
    }

    @Override
    public boolean update(DnDEvent event) {
        event.setDropPossible(false, "");

        final List<InstanceItem> movingItems = getDropInstance(event);
        if (movingItems.isEmpty()) {
            return false;
        }

        final DnDPosition dnDPosition = getDnDPosition(event);
        if (dnDPosition == null) {
            return false;
        }

        final DnDPlace place = dnDPosition.place;
        final Rectangle bound = dnDPosition.bound;
        final InstanceItem newParent = dnDPosition.item;

        InstanceItem i = newParent;
        while (i != null) {
            if (movingItems.contains(i)) {
                return false;
            }
            i = i.getParent();
        }

        if (place == DnDPlace.INSIDE) {
            if (!(newParent instanceof StructuralItem)) {
                return false;
            }
            event.setDropPossible(true, "");
            event.setHighlighting(new RelativeRectangle(this, bound), DnDEvent.DropTargetHighlightingType.RECTANGLE);
        } else if (place == DnDPlace.AFTER) {
            event.setDropPossible(true, "");
            bound.y = bound.y + bound.height;
            bound.height = 1;
            event.setHighlighting(new RelativeRectangle(this, bound), DnDEvent.DropTargetHighlightingType.RECTANGLE);
        } else if (place == DnDPlace.BEFORE) {
            bound.height = 1;
            event.setDropPossible(true, "");
            event.setHighlighting(new RelativeRectangle(this, bound), DnDEvent.DropTargetHighlightingType.RECTANGLE);
        }
        return true;
    }

    @NotNull
    private List<InstanceItem> getDropInstance(DnDEvent event) {
        final Object attachedObject = event.getAttachedObject();
        if (attachedObject instanceof List<?> l) {
            return l.stream().filter(i -> i instanceof InstanceItem).map(i -> (InstanceItem) i).toList();
        } else if (attachedObject instanceof DnDNativeTarget.EventInfo nt) {
            final String text = nt.getTextForFlavor(DataFlavor.stringFlavor);
            if (text == null) {
                return List.of();
            }
            final KdbInstance instance = KdbInstance.parseInstance(text);
            if (instance == null) {
                return List.of();
            }
            return List.of(instance);
        }
        return List.of();
    }

    private DnDPosition getDnDPosition(DnDEvent event) {
        final Point point = event.getPointOn(this);
        final TreePath path = getClosestPathForLocation(point.x, point.y);
        if (path == null) {
            return null;
        }

        final InstanceItem item = (InstanceItem) TreeUtil.getLastUserObject(path);
        final Rectangle bound = getPathBounds(path);
        if (item == null || bound == null) {
            return null;
        }

        final int anchor = bound.height / 3;
        if (point.y > bound.y + bound.height - anchor) {
            return item instanceof KdbScope ? null : new DnDPosition(DnDPlace.AFTER, item, bound);
        } else if (point.y < bound.y + anchor) {
            return item instanceof KdbScope ? null : new DnDPosition(DnDPlace.BEFORE, item, bound);
        }
        return new DnDPosition(DnDPlace.INSIDE, item, bound);
    }

    @Override
    public void performPaste(@NotNull DataContext dataContext) {
        final StructuralItem target = getPasteTargetElement();
        if (target == null) {
            return;
        }

        final boolean copy = model.releaseCutItems().isEmpty();
        final List<InstanceItem> pasteItems = CopyPasteManager.getInstance().getContents(TransferableItems.DATA_FLAVOR);
        if (pasteItems == null) {
            return;
        }

        final List<InstanceItem> res = new ArrayList<>(pasteItems.size());
        for (InstanceItem pasteItem : pasteItems) {
            final InstanceItem e;
            if (copy) {
                e = target.copyItem(pasteItem);
            } else {
                e = target.moveItem(pasteItem);
            }
            res.add(e);
        }
        expandItem(target);
        selectItems(res);
    }

    @Override
    public void performCut(@NotNull DataContext dataContext) {
        final List<InstanceItem> items = getCutCopyElements();
        if (items != null) {
            model.setCuttingItems(items);
            CopyPasteManager.getInstance().setContents(new TransferableItems(items));
        }
    }

    @Override
    public void performCopy(@NotNull DataContext dataContext) {
        model.setCuttingItems(null);
        final List<InstanceItem> items = getCutCopyElements();
        if (items != null) {
            CopyPasteManager.getInstance().setContents(new TransferableItems(items));
        }
    }

    @Override
    public void deleteElement(@NotNull DataContext dataContext) {
        if (!MessageDialogBuilder.yesNo("Are you sure?", "Selected items will be removed and it can't be cancelled").guessWindowAndAsk()) {
            return;
        }
        model.setCuttingItems(null);
        getSelectedItems().forEach(item -> item.getParent().removeItem(item));
    }

    @Override
    public boolean isPastePossible(@NotNull DataContext dataContext) {
        return getPasteTargetElement() != null;
    }

    @Override
    public boolean isPasteEnabled(@NotNull DataContext dataContext) {
        return isPastePossible(dataContext);
    }

    @Override
    public boolean isCopyEnabled(@NotNull DataContext dataContext) {
        return getCutCopyElements() != null;
    }

    @Override
    public boolean isCopyVisible(@NotNull DataContext dataContext) {
        return getCutCopyElements() != null;
    }

    @Override
    public boolean isCutEnabled(@NotNull DataContext dataContext) {
        return getCutCopyElements() != null;
    }

    @Override
    public boolean isCutVisible(@NotNull DataContext dataContext) {
        return getCutCopyElements() != null;
    }

    @Override
    public boolean canDeleteElement(@NotNull DataContext dataContext) {
        return getCutCopyElements() != null;
    }

    private void cancelCutCopy() {
        model.setCuttingItems(null);
    }

    private List<InstanceItem> getCutCopyElements() {
        final List<InstanceItem> selectedItems = getSelectedItems();
        if (selectedItems.isEmpty() || selectedItems.stream().anyMatch(i -> i instanceof KdbScope)) {
            return null;
        }
        return selectedItems;
    }

    private StructuralItem getPasteTargetElement() {
        if (!CopyPasteManager.getInstance().areDataFlavorsAvailable(TransferableItems.DATA_FLAVOR)) {
            return null;
        }

        final List<InstanceItem> selectedItems = getSelectedItems();
        if (selectedItems.size() != 1) {
            return null;
        }

        final InstanceItem item = selectedItems.get(0);
        if (item instanceof StructuralItem) {
            return (StructuralItem) item;
        }
        return null;
    }

    @Override
    public void dispose() {
        Disposer.dispose(model);
        scope.removeScopeListener(model);
        manager.removeQueryListener(managerListener);
        manager.removeConnectionListener(managerListener);
        ToolTipManager.sharedInstance().unregisterComponent(this);
    }

    public void showConnectionDetails(boolean state) {
        cellRenderer.showConnectionDetails(state);
    }

    public boolean isShownConnectionDetails() {
        return cellRenderer.isShownConnectionDetails();
    }

    public JComponent getSearchComponent() {
        return searchSession.getComponent();
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (PlatformDataKeys.CUT_PROVIDER.is(dataId)) {
            return this;
        }
        if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
            return this;
        }
        if (PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
            return this;
        }
        if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
            return this;
        }
        return null;
    }

    public void showSpeedSearch() {
        if (searchSession.isOpened()) {
            searchSession.close();
        }
        speedSearch.showPopup();
    }

    public void showSearchAndReplace(boolean replace) {
        if (speedSearch.isPopupActive()) {
            speedSearch.hidePopup();
        }
        searchSession.open(replace);
    }

    private class TheManagerListener implements KdbConnectionListener, KdbQueryListener {
        @Override
        public void queryStarted(InstanceConnection connection, KdbQuery query) {
            updateInstanceItem(connection);
        }

        @Override
        public void queryFinished(InstanceConnection connection, KdbQuery query, KdbResult result) {
            updateInstanceItem(connection);
        }

        @Override
        public void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
            updateInstanceItem(connection);
        }

        private void updateInstanceItem(InstanceConnection connection) {
            if (connection.isTemporal()) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                final KdbInstance instance = connection.getInstance();
                if (instance.getScope() == scope) {
                    model.itemUpdated(scope, instance);
                }
            });
        }
    }

    private enum DnDPlace {
        BEFORE,
        AFTER,
        INSIDE
    }

    private record DnDPosition(DnDPlace place, InstanceItem item, Rectangle bound) {
    }
}

