package org.kdb.inside.brains.psi;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.IconManager;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.BitUtil;
import com.intellij.util.VisibilityIcons;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY;

public class QIconProvider extends IconProvider implements DumbAware {
    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        final boolean visibility = BitUtil.isSet(flags, ICON_FLAG_VISIBILITY);

        if (element instanceof QImport) {
            return KdbIcons.Node.Import;
        } else if (element instanceof QCommand) {
            return KdbIcons.Node.Command;
        } else if (element instanceof QContext) {
            return KdbIcons.Node.Context;
        } else if (element instanceof QTableColumn) {
            final QTableColumn col = (QTableColumn) element;
            final boolean keys = col.getParent() instanceof QTableKeys;
            return keys ? KdbIcons.Node.TableKeyColumn : KdbIcons.Node.TableValueColumn;
        } else if (element instanceof QLambdaExpr) {
            return KdbIcons.Node.Lambda;
        } else if (element instanceof QAssignmentExpr) {
            final QAssignmentExpr assignment = (QAssignmentExpr) element;
            return getAssignmentIcon(assignment, visibility);
        }
        return null;
    }

    private Icon getAssignmentIcon(QAssignmentExpr assignment, boolean visibility) {
        final QExpression expression = assignment.getExpression();
        if (expression == null) {
            return null;
        }

        Icon i = getExpressionIcon(expression);
        if (visibility) {
            final RowIcon icon = IconManager.getInstance().createLayeredIcon(assignment, i, 0);
            if (QPsiUtil.isGlobalDeclaration(assignment)) {
                VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PUBLIC, icon);
            } else {
                VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PRIVATE, icon);
            }
            return icon;
        }
        return i;
    }

    private Icon getExpressionIcon(QExpression expression) {
        if (expression instanceof QLambdaExpr) {
            return KdbIcons.Node.Lambda;
        } else if (expression instanceof QTableExpr) {
            return KdbIcons.Node.Table;
        }
        return KdbIcons.Node.Variable;
    }
}
