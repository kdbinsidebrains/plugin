package org.kdb.inside.brains.core;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Objects;

public abstract class InstanceItem implements Transferable {
    private String name;
    private Color color;

    protected StructuralItem parent;

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(InstanceItem.class, "Kdb Instance Item");

    public InstanceItem(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public String getCanonicalName() {
        if (parent == null) {
            return getName();
        }
        return parent.getCanonicalName() + '/' + getName();
    }

    public void setName(String name) {
        Objects.requireNonNull(name, "Name can't be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name can't be empty");
        }
        if (!Objects.equals(this.name, name)) {
            this.name = name;
            notifyItemUpdated();
        }
    }

    public Color getColor() {
        return color;
    }

    public Color getInheritedColor() {
        if (color != null) {
            return color;
        }
        return parent != null ? parent.getInheritedColor() : null;
    }

    public void setColor(Color color) {
        if (!Objects.equals(this.color, color)) {
            this.color = color;
            notifyItemUpdated();
        }
    }

    public StructuralItem getParent() {
        return parent;
    }

    public KdbScope getScope() {
        InstanceItem item = this;
        while (item != null && !(item instanceof KdbScope)) {
            item = item.getParent();
        }
        return (KdbScope) item;
    }

    public TreePath getTreePath() {
        int depth = 1;
        for (StructuralItem p = parent; p != null; p = p.parent) {
            depth++;
        }

        StructuralItem p = parent;
        final InstanceItem[] path = new InstanceItem[depth];
        path[depth - 1] = this;
        for (int i = depth - 2; i >= 0; i--, p = p.parent) {
            path[i] = p;
        }
        return new TreePath(path);
    }

    protected void notifyItemUpdated() {
        final KdbScope scope = getScope();
        if (scope != null) {
            scope.processItemUpdated(this);
        }
    }

    public abstract InstanceItem copy();

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{
                DATA_FLAVOR
        };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DATA_FLAVOR);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return this;
    }

    @Override
    public String toString() {
        // WARNING: This method is used to store in TreeState. Don't override it!
        return name;
    }
}
