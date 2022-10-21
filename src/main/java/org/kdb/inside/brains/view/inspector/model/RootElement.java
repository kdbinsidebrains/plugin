package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RootElement implements StructureViewTreeElement, ItemPresentation {
    private TreeElement[] children = EMPTY_ARRAY;

    public RootElement() {
    }

    void updateInstance(InstanceElement element) {
        this.children = element == null ? EMPTY_ARRAY : new InstanceElement[]{element};
    }

    @Override
    public Object getValue() {
        return children;
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        return children;
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

    @Override
    public @Nullable String getPresentableText() {
        return null;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return null;
    }
}