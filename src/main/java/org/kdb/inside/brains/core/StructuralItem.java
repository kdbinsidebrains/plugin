package org.kdb.inside.brains.core;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class StructuralItem extends InstanceItem implements Iterable<InstanceItem> {
    protected final List<InstanceItem> children = new ArrayList<>();

    StructuralItem(@NotNull String name) {
        super(name);
    }

    public PackageItem createPackage(@NotNull String name) {
        final PackageItem p = new PackageItem(name);
        insertChild(p, children.size());
        return p;
    }

    public KdbInstance createInstance(@NotNull String name, @NotNull String host, int port, String credentials, @NotNull InstanceOptions options) {
        final KdbInstance i = new KdbInstance(name, host, port, credentials, options);
        insertChild(i, children.size());
        return i;
    }

    public InstanceItem copyItem(@NotNull InstanceItem item) {
        return copyItem(item, children.size());
    }

    public InstanceItem copyItem(@NotNull InstanceItem item, int pos) {
        if (!(item instanceof PackageItem) && !(item instanceof KdbInstance)) {
            throw new IllegalArgumentException("Item must be PackageItem or KdbInstance");
        }
        final InstanceItem copy = item.copy();
        insertChild(copy, pos);
        return copy;
    }

    public InstanceItem moveItem(@NotNull InstanceItem item) {
        return moveItem(item, children.size());
    }

    public InstanceItem moveItem(@NotNull InstanceItem item, int pos) {
        if (!(item instanceof PackageItem) && !(item instanceof KdbInstance)) {
            throw new IllegalArgumentException("Item must be PackageItem or KdbInstance");
        }
        insertChild(item, pos);
        return item;
    }

    public void removeItem(@NotNull InstanceItem item) {
        removeChild(item);
    }

    private void insertChild(@NotNull InstanceItem item, int pos) {
        final int index = childIndex(item);

        // Nothing to change
        if (item.parent == this && index == pos) {
            return;
        }

        if (item.parent != null) {
            item.parent.removeChild(item);
        }

        fixNameIfRequired(item);

        item.parent = this;
        if (index == -1 || index >= pos) {
            children.add(pos, item);
        } else {
            children.add(pos - 1, item);
        }

        notifyChildAdded(item, children.indexOf(item));
    }

    private void removeChild(InstanceItem item) {
        final int i = children.indexOf(item);
        if (i < 0) {
            return;
        }
        children.remove(i);
        item.parent = null;
        notifyChildRemoved(item, i);
    }

    public int childIndex(InstanceItem item) {
        return children.indexOf(item);
    }

    public InstanceItem getChild(int i) {
        return children.get(i);
    }

    public List<InstanceItem> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getChildrenCount() {
        return children.size();
    }

    public InstanceItem findByName(String name) {
        return children.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    protected final void notifyChildAdded(InstanceItem child, int index) {
        final KdbScope scope = getScope();
        if (scope != null) {
            scope.processItemAdded(this, child, index);
        }
    }

    protected final void notifyChildRemoved(InstanceItem child, int index) {
        final KdbScope scope = getScope();
        if (scope != null) {
            scope.processItemRemoved(this, child, index);
        }
    }

    @NotNull
    @Override
    public Iterator<InstanceItem> iterator() {
        return children.iterator();
    }

    private void fixNameIfRequired(InstanceItem item) {
        final Set<String> collect = children.stream().map(InstanceItem::getName).collect(Collectors.toSet());

        String name = item.getName();
        while (collect.contains(name)) {
            name += " Copy";
        }
        item.setName(name);
    }
}
