package org.kdb.inside.brains.psi;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.util.BitUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY;

public class QIconProvider extends IconProvider implements DumbAware {
    public static Icon getColumnIcon(@NotNull QTableColumn column) {
        return QPsiUtil.isKeyColumn(column) ? KdbIcons.Node.TableKeyColumn : KdbIcons.Node.TableValueColumn;
    }

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof QFile) {
            return KdbIcons.Node.File;
        } else if (element instanceof QImport) {
            return KdbIcons.Node.Import;
        } else if (element instanceof QCommand) {
            return KdbIcons.Node.Command;
        } else if (element instanceof QContext) {
            return KdbIcons.Node.Context;
        } else if (element instanceof QSymbol) {
            return KdbIcons.Node.Symbol;
        } else if (element instanceof QTableColumn tbl) {
            return getColumnIcon(tbl);
        } else if (element instanceof QLambdaExpr) {
            return KdbIcons.Node.Lambda;
        } else if (element instanceof QAssignmentExpr assignment) {
            return getAssignmentIcon(assignment, BitUtil.isSet(flags, ICON_FLAG_VISIBILITY));
        } else if (element instanceof QVarDeclaration declaration) {
            return getIcon(declaration.getParent(), flags);
        }
        return null;
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
            return KdbIcons.Node.Lambda;
        } else if (expression instanceof QTableExpr) {
            return KdbIcons.Node.Table;
        }
        return KdbIcons.Node.Variable;
    }

    private Icon getExpressionIcon(QExpression expression, boolean global) {
        if (expression instanceof QLambdaExpr) {
            return global ? KdbIcons.Node.LambdaPublic : KdbIcons.Node.LambdaPrivate;
        } else if (expression instanceof QTableExpr) {
            return global ? KdbIcons.Node.TablePublic : KdbIcons.Node.TablePrivate;
        }
        return global ? KdbIcons.Node.VariablePublic : KdbIcons.Node.VariablePrivate;
    }
}
