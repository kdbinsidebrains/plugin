package org.kdb.inside.brains.view.struct;

import icons.KdbIcons;

import javax.swing.*;

public enum StructureElementType {
    FILE(KdbIcons.Node.File, false, true),
    IMPORT(KdbIcons.Node.Import, true, false),
    COMMAND(KdbIcons.Node.Command, true, false),
    CONTEXT(KdbIcons.Node.Context, false, true),
    SYMBOL(KdbIcons.Node.Symbol, true, false),
    VARIABLE(KdbIcons.Node.Variable, true, false),
    LAMBDA(KdbIcons.Node.Lambda, false, true),
    DICT(KdbIcons.Node.Dict, false, false),
    DICT_FIELD(KdbIcons.Node.DictField, true, false),
    TABLE(KdbIcons.Node.Table, false, false),
    TABLE_KEY_COLUMN(KdbIcons.Node.TableKeyColumn, true, false),
    TABLE_VALUE_COLUMN(KdbIcons.Node.TableValueColumn, true, false);

    private final Icon icon;
    private final boolean alwaysLeaf;
    private final boolean autoExpand;

    StructureElementType(Icon icon, boolean alwaysLeaf, boolean autoExpand) {
        this.icon = icon;
        this.alwaysLeaf = alwaysLeaf;
        this.autoExpand = autoExpand;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isAlwaysLeaf() {
        return alwaysLeaf;
    }

    public boolean isAutoExpand() {
        return autoExpand;
    }
}
