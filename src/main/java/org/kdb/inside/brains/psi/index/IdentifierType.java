package org.kdb.inside.brains.psi.index;

import icons.KdbIcons;

import javax.swing.*;

public enum IdentifierType {
    TABLE(KdbIcons.Node.tablePublic),
    LAMBDA(KdbIcons.Node.functionPublic),
    VARIABLE(KdbIcons.Node.variablePublic);

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
