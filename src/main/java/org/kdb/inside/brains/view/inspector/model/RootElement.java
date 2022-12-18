package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RootElement implements StructureViewTreeElement, ItemPresentation {
    private InstanceElement element;

    public RootElement() {
    }

    void updateInstance(InstanceElement element) {
        this.element = element;
    }

    @Override
    public InstanceElement getValue() {
        return element;
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
        return element == null ? EMPTY_ARRAY : new TreeElement[]{element};
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