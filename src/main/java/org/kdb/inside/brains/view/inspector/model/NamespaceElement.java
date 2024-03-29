package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NamespaceElement extends InspectorElement {
    private final Object[] item;

    public NamespaceElement(String namespace, Object[] item) {
        super((String) item[0], namespace == null ? "" : namespace, KdbIcons.Node.Namespace);
        this.item = item;
    }

    protected static InspectorElement[] buildChildren(NamespaceElement element, Object[] item) {
        final String namespace = element == null ? null : element.getCanonicalName();

        final List<InspectorElement> children = new ArrayList<>();
        Stream.of((Object[]) item[1]).forEach(s -> children.add(new FunctionElement(namespace, (Object[]) s)));
        Stream.of((Object[]) item[2]).forEach(s -> children.add(new TableElement(namespace, (Object[]) s)));
        Stream.of((Object[]) item[3]).forEach(s -> children.add(new VariableElement(namespace, (Object[]) s)));
        Stream.of((Object[]) item[4]).forEach(s -> children.add(new NamespaceElement(namespace, (Object[]) s)));
        return children.toArray(InspectorElement[]::new);
    }

    @Override
    protected InspectorElement[] buildChildren() {
        return buildChildren(this, item);
    }
}