package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import icons.KdbIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NamespaceElement extends InspectorElement {
    private final Object[] item;

    public NamespaceElement(Object[] item) {
        super((String) item[0], KdbIcons.Node.Namespace);
        this.item = item;
    }

    @Override
    protected TreeElement[] buildChildren() {
        final List<TreeElement> children = new ArrayList<>();
        Stream.of((String[]) item[1]).forEach(s -> children.add(new FunctionElement(s)));
        Stream.of((Object[]) item[2]).forEach(s -> children.add(new TableElement((Object[]) s)));
        Stream.of((Object[]) item[3]).forEach(s -> children.add(new VariableElement((Object[]) s)));
        Stream.of((Object[]) item[4]).forEach(s -> children.add(new NamespaceElement((Object[]) s)));
        return children.toArray(TreeElement[]::new);
    }
}
