package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.KdbScopeListener;
import org.kdb.inside.brains.core.StructuralItem;

import javax.swing.tree.TreePath;
import java.util.*;

public class InstancesTreeModel extends BaseTreeModel<InstanceItem> implements InvokerSupplier, KdbScopeListener {
    private final KdbScope scope;
    private final Invoker invoker;

    private final Set<InstanceItem> cuttingItems = new HashSet<>();

    public InstancesTreeModel(KdbScope scope) {
        this.scope = scope;
        this.invoker = Invoker.forBackgroundThreadWithReadAction(this);
        Disposer.register(this, invoker);
    }

    @Override
    public KdbScope getRoot() {
        return scope;
    }

    @NotNull
    @Override
    public Invoker getInvoker() {
        return invoker;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        throw new UnsupportedOperationException("editable tree have to implement TreeModel#valueForPathChanged");
    }

    @Override
    public List<? extends InstanceItem> getChildren(Object parent) {
        if (parent instanceof StructuralItem) {
            return ((StructuralItem) parent).getChildren();
        }
        return null;
    }

    @Override
    public void itemUpdated(KdbScope scope, InstanceItem item) {
        final StructuralItem parent = item.getParent();
        if (parent == null) {
            treeStructureChanged(null, null, null);
        } else {
            treeNodesChanged(parent.getTreePath(), new int[]{parent.childIndex(item)}, new Object[]{item});
        }
    }

    @Override
    public void itemCreated(KdbScope scope, StructuralItem parent, InstanceItem child, int index) {
        if (parent == null) {
            treeStructureChanged(null, null, null);
        } else {
            treeNodesInserted(parent.getTreePath(), new int[]{index}, new Object[]{child});
        }
    }

    @Override
    public void itemRemoved(KdbScope scope, StructuralItem parent, InstanceItem child, int index) {
        if (parent == null) {
            treeStructureChanged(null, null, null);
        } else {
            treeNodesRemoved(parent.getTreePath(), new int[]{index}, new Object[]{child});
        }
    }

    public Collection<InstanceItem> releaseCutItems() {
        List<InstanceItem> a = new ArrayList<>(cuttingItems);
        setCuttingItems(null);
        return a;
    }

    public void setCuttingItems(Collection<InstanceItem> items) {
        if (!cuttingItems.isEmpty()) {
            final Set<InstanceItem> cp = new HashSet<>(cuttingItems);
            cuttingItems.clear();

            for (InstanceItem item : cp) {
                final StructuralItem parent = item.getParent();
                treeNodesChanged(parent.getTreePath(), new int[]{parent.childIndex(item)}, new Object[]{item});
            }
        }

        if (items != null) {
            this.cuttingItems.addAll(items);
            for (InstanceItem item : items) {
                final StructuralItem parent = item.getParent();
                treeNodesChanged(parent.getTreePath(), new int[]{parent.childIndex(item)}, new Object[]{item});
            }
        }
    }

    public boolean isCuttingItem(InstanceItem item) {
        return cuttingItems.contains(item);
    }

    @Override
    public void dispose() {
        super.dispose();
        invoker.dispose();
        cuttingItems.clear();
    }
}
