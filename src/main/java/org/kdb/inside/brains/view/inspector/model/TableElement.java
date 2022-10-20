package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import kx.c;

public class TableElement extends ExecutableElement {
    private final long size;
    private final c.Flip meta;

    public TableElement(Object[] item) {
        super((String) item[0], KdbIcons.Node.Table);
        size = (Long) item[1];
        meta = (c.Flip) item[2];
    }
}
