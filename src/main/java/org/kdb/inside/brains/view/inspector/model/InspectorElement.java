package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class InspectorElement implements StructureViewTreeElement, ItemPresentation {
    private final String name;
    private final Icon icon;

    private TreeElement[] children;

    public InspectorElement(String name, Icon icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    @Override
    public final @Nullable String getPresentableText() {
        return getName();
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public final Object getValue() {
        return this;
    }

    @Override
    public final TreeElement @NotNull [] getChildren() {
        if (children == null) {
            children = buildChildren();
        }
        return children;
    }

    protected TreeElement[] buildChildren() {
        return EMPTY_ARRAY;
    }

    @Override
    public final @NotNull ItemPresentation getPresentation() {
        return this;
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
}