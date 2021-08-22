package org.kdb.inside.brains.psi.index;

import icons.KdbIcons;
import org.kdb.inside.brains.psi.QAssignment;
import org.kdb.inside.brains.psi.QExpression;

import javax.swing.*;

public enum IdentifierType {
    TABLE(KdbIcons.Node.Table),
    LAMBDA(KdbIcons.Node.Lambda),
    VARIABLE(KdbIcons.Node.Variable);

    private final Icon icon;

    IdentifierType(Icon icon) {
        this.icon = icon;
    }

    public static IdentifierType getType(QAssignment assignment) {
        final QExpression expression = assignment.getExpression();
        if (expression == null) {
            return VARIABLE;
        }

        if (!expression.getLambdaList().isEmpty()) {
            return LAMBDA;
        }

        if (!expression.getTableList().isEmpty()) {
            return TABLE;
        }

        return VARIABLE;
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
