package org.kdb.inside.brains.view.inspector.model;

import org.jetbrains.annotations.NotNull;

public class RootElement extends InspectorElement {
    private InstanceElement element;

    public RootElement() {
        super(null, null, null);
    }

    public InstanceElement getElement() {
        return element;
    }

    void updateInstance(InstanceElement element) {
        this.element = element;
    }

    @Override
    public InspectorElement @NotNull [] getChildren() {
        return element == null ? EMPTY_ARRAY : new InspectorElement[]{element};
    }
}