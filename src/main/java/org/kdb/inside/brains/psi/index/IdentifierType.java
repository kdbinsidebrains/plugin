package org.kdb.inside.brains.psi.index;

import icons.KdbIcons;

import javax.swing.*;

public enum IdentifierType {
    TABLE(KdbIcons.Node.Table),
    LAMBDA(KdbIcons.Node.Lambda),
    VARIABLE(KdbIcons.Node.Variable);

    private final Icon icon;

    IdentifierType(Icon icon) {
        this.icon = icon;
    }

    public static IdentifierType parseFrom(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Icon getIcon() {
        return icon;
    }
}
