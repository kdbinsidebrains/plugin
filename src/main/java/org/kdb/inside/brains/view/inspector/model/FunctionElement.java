package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;

public class FunctionElement extends ExecutableElement {
    public FunctionElement(String name) {
        super(name, KdbIcons.Node.Function);
    }
}
