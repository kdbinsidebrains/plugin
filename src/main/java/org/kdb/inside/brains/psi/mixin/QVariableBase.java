package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;

public class QVariableBase extends QPsiElementImpl implements QVariable, ItemPresentation {
    private String name = null;

    public QVariableBase(ASTNode node) {
        super(node);
    }

    @NotNull
    public String getName() {
        if (name == null) {
            name = getText();
            if (name.endsWith("IntellijIdeaRulezzz")) {
                name = name.substring(0, name.length() - 19);
            }
        }
        return name;
    }

    @Override
    public String getPresentableText() {
        return getQualifiedName();
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Variable;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return KdbIcons.Node.Variable;
    }

    @Override
    public String getLocationString() {
        return QPsiUtil.getLocationString(this);
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        return calculateQualifiedName();
    }

    @Override
    public ElementContext getVariableContext() {
        return ElementContext.of(this);
    }

    @NotNull
    private String calculateQualifiedName() {
        final String name = getName();

        // if has namespace - it's global in any case
        if (QPsiUtil.hasNamespace(name)) {
            return name;
        }

        // It's namespace name itself or table column - ignore
        final PsiElement parent = getParent();
        if (parent instanceof QContext || parent instanceof QTableColumn) {
            return name;
        }

        // No namespace - ignore
        final QContext context = PsiTreeUtil.getParentOfType(this, QContext.class);
        if (context == null || context.getVariable() == null) {
            return name;
        }

        // root namespace - ignore
        final String namespaceName = context.getVariable().getName();
        if (".".equals(namespaceName)) {
            return name;
        }

        // no lambda - return full name
        final QLambdaExpr lambda = getContext(QLambdaExpr.class);
        if (lambda != null && QPsiUtil.isInnerDeclaration(lambda, name)) {
            return name;
        }
        return QPsiUtil.createQualifiedName(namespaceName, name);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        invalidate();
    }

    public void invalidate() {
        name = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }
}
