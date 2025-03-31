package org.kdb.inside.brains.psi.index;

import com.intellij.lang.LighterASTNode;
import com.intellij.psi.tree.IElementType;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;

import static org.kdb.inside.brains.psi.QTypes.*;

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

    public Icon getIcon() {
        return icon;
    }

    public static @Nullable IdentifierType parseFrom(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static @NotNull IdentifierType getType(QAssignmentExpr assignment) {
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

    public static @NotNull IdentifierType getType(@Nullable LighterASTNode expression) {
        if (expression == null) {
            return IdentifierType.VARIABLE;
        }

        final IElementType tt = expression.getTokenType();
        if (tt == LAMBDA_EXPR) {
            return IdentifierType.LAMBDA;
        }
        if (tt == TABLE_EXPR) {
            return IdentifierType.TABLE;
        }
        if (tt == DICT_EXPR) {
            return IdentifierType.DICT;
        }
        return IdentifierType.VARIABLE;
    }
}
