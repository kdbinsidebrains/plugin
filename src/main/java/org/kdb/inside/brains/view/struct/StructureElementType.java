package org.kdb.inside.brains.view.struct;

import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.IconManager;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.VisibilityIcons;
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
    private final RowIcon iconPublic;
    private final RowIcon iconPrivate;
    private final boolean alwaysLeaf;
    private final boolean autoExpand;

    StructureElementType(Icon icon, boolean alwaysLeaf, boolean autoExpand) {
        this.icon = icon;
        this.alwaysLeaf = alwaysLeaf;
        this.autoExpand = autoExpand;

        final IconManager instance = IconManager.getInstance();
        iconPublic = instance.createRowIcon(2);
        iconPublic.setIcon(icon, 0);
        VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PUBLIC, iconPublic);

        iconPrivate = instance.createRowIcon(2);
        iconPrivate.setIcon(icon, 0);
        VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PRIVATE, iconPrivate);
    }

    public Icon getIcon() {
        return icon;
    }

    public Icon getIcon(boolean global) {
        return global ? iconPublic : iconPrivate;
    }

    public boolean isAlwaysLeaf() {
        return alwaysLeaf;
    }

    public boolean isAutoExpand() {
        return autoExpand;
    }
}
