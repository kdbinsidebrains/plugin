package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import org.jetbrains.annotations.NotNull;

public class RootElement extends InspectorElement {
    private NamespaceElement element;

    public RootElement() {
        super("This is root element", null);
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        return element == null ? TreeElement.EMPTY_ARRAY : new TreeElement[]{element};
    }

    void updateNamespaces(NamespaceElement element) {
        this.element = element;
    }

    @Override
    public boolean isAlwaysLeaf() {
        return false;
    }
}