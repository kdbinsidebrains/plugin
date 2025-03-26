package org.kdb.inside.brains.psi.index;

import icons.KdbIcons;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;

public enum IdentifierType {
    DICT(KdbIcons.Node.Dict),
    TABLE(KdbIcons.Node.Table),
    SYMBOL(KdbIcons.Node.Symbol),
    LAMBDA(KdbIcons.Node.Lambda),
    VARIABLE(KdbIcons.Node.Variable),
    ARGUMENT(KdbIcons.Node.Parameter);

    private final Icon icon;

    IdentifierType(Icon icon) {
        this.icon = icon;
    }

    public static IdentifierType getType(QAssignmentExpr assignment) {
        final QExpression expression = assignment.getExpression();
        if (expression instanceof QLambdaExpr) {
            return LAMBDA;
        }
        if (expression instanceof QDictExpr) {
            return DICT;
        }
        if (expression instanceof QTableExpr) {
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
