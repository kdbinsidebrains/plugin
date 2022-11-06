package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import kx.c;

import java.lang.reflect.Array;

public class TableElement extends ExecutableElement {
    private final long size;
    private final c.Flip meta;
    private String location;

    public TableElement(String namespace, Object[] item) {
        super((String) item[0], namespace, KdbIcons.Node.Table);
        size = ((Number) item[1]).longValue();
        meta = (c.Flip) item[2];
        location = Array.getLength(meta.y[0]) + " columns, " + size + " rows, " + (isHistorical() ? "historical" : "memorable");
    }

    @Override
    public String getLocationString() {
        return location;
    }

    public boolean isHistorical() {
        return "date".equals(Array.get(meta.y[0], 0));
    }
}