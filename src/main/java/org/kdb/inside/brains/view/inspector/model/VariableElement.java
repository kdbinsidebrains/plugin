package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;

public class VariableElement extends InspectorElement implements ExecutableElement {
    private final short type;

    public VariableElement(Object[] item) {
        super((String) item[0], KdbIcons.Node.Variable);
        type = (Short) item[1];
    }

    @Override
    public String getQuery() {
        return getPresentableText();
    }
}
