package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class InspectorElement implements StructureViewTreeElement, ItemPresentation {
    public static final TreeElement[] NO_CHILDREN = new TreeElement[0];
    private final String name;
    private final Icon icon;

    public InspectorElement(String name) {
        this(name, null);
    }

    public InspectorElement(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        return NO_CHILDREN;
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public @Nullable String getPresentableText() {
        return name;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public Object getValue() {
        return name;
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    public boolean isAlwaysLeaf() {
        return true;
    }
}