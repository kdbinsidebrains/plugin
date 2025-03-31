package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.kdb.inside.brains.psi.QPatternDeclaration;
import org.kdb.inside.brains.psi.QPsiElementImpl;
import org.kdb.inside.brains.psi.QTypedVariable;
import org.kdb.inside.brains.psi.QTypedVariables;

import java.util.ArrayList;
import java.util.List;

import static org.kdb.inside.brains.psi.QTypes.SEMICOLON;

public abstract class QPatternDeclarationMixin extends QPsiElementImpl implements QPatternDeclaration, QTypedVariables {
    public QPatternDeclarationMixin(ASTNode node) {
        super(node);
    }

    @Override
    public List<QTypedVariable> getOrderedTypedVariables() {
        PsiElement re = getFirstChild();
        final List<QTypedVariable> res = new ArrayList<>();
        QTypedVariable curr = null;
        while (re != null) {
            if (re instanceof QTypedVariable tv) {
                curr = tv;
            } else {
                final IElementType tokenType = re.getNode().getElementType();
                if (tokenType == SEMICOLON) {
                    res.add(curr);
                    curr = null;
                }
            }
            re = re.getNextSibling();
        }
        res.add(curr);
        return res;
    }
}
