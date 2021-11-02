package org.kdb.inside.brains.psi.index;

import icons.KdbIcons;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QExpression;
import org.kdb.inside.brains.psi.QLambdaExpr;
import org.kdb.inside.brains.psi.QTableExpr;

import javax.swing.*;

public enum IdentifierType {
    TABLE(KdbIcons.Node.Table),
    SYMBOL(KdbIcons.Node.Symbol),
    LAMBDA(KdbIcons.Node.Lambda),
    VARIABLE(KdbIcons.Node.Variable);

    private final Icon icon;

    IdentifierType(Icon icon) {
        this.icon = icon;
    }

    public static IdentifierType getType(QAssignmentExpr assignment) {
        final QExpression expression = assignment.getExpression();
        if (expression instanceof QLambdaExpr) {
            return LAMBDA;
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
