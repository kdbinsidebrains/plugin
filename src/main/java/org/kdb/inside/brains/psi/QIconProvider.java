package org.kdb.inside.brains.psi;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY;
import static icons.KdbIcons.Node.*;

public class QIconProvider extends IconProvider implements DumbAware {
    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof QFile) {
            return File;
        } else if (element instanceof QImport) {
            return Import;
        } else if (element instanceof QCommand) {
            return Command;
        } else if (element instanceof QContext) {
            return Context;
        } else if (element instanceof QSymbol) {
            return Symbol;
        } else if (element instanceof QTableColumn tbl) {
            return getColumnIcon(tbl);
        } else if (element instanceof QLambdaExpr) {
            return Lambda;
        } else if (element instanceof QAssignmentExpr assignment) {
            return getAssignmentIcon(assignment, BitUtil.isSet(flags, ICON_FLAG_VISIBILITY));
        } else if (element instanceof QVarDeclaration declaration) {
            return getIcon(declaration.getParent(), flags);
        }
        return null;
    }

    public static @NotNull Icon getColumnIcon(QTableColumn column) {
        final PsiElement parent = column.getParent();
        if (parent instanceof QTableKeys) {
            if (parent.getParent() instanceof QDictExpr) {
                return DictField;
            } else {
                return TableKeyColumn;
            }
        } else {
            return TableValueColumn;
        }
    }

    private Icon getAssignmentIcon(QAssignmentExpr assignment, boolean visibility) {
        final QExpression expression = assignment.getExpression();
        if (expression == null) {
            return null;
        }

        if (visibility) {
            final boolean global = QPsiUtil.isGlobalDeclaration(assignment);
            return getExpressionIcon(expression, global);
        } else {
            return getExpressionIcon(expression);
        }
    }

    private Icon getExpressionIcon(QExpression expression) {
        if (expression instanceof QLambdaExpr) {
            return Lambda;
        } else if (expression instanceof QTableExpr) {
            return Table;
        } else if (expression instanceof QDictExpr) {
            return Dict;
        }
        return Variable;
    }

    private Icon getExpressionIcon(QExpression expression, boolean global) {
        if (expression instanceof QLambdaExpr) {
            return global ? LambdaPublic : LambdaPrivate;
        } else if (expression instanceof QTableExpr) {
            return global ? TablePublic : TablePrivate;
        } else if (expression instanceof QDictExpr) {
            return global ? DictPublic : DictPrivate;
        }
        return global ? VariablePublic : VariablePrivate;
    }
}
