package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QDictExpr;
import org.kdb.inside.brains.psi.QPsiElementImpl;
import org.kdb.inside.brains.psi.QTableColumn;
import org.kdb.inside.brains.psi.QTableValues;

import javax.swing.*;

public abstract class QTableColumnMixin extends QPsiElementImpl implements QTableColumn {
    public QTableColumnMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        final PsiElement parent = getParent();
        if (parent instanceof QTableValues) {
            return KdbIcons.Node.TableValueColumn;
        } else {
            return parent.getParent() instanceof QDictExpr ? KdbIcons.Node.DictField : KdbIcons.Node.TableKeyColumn;
        }
    }
}
