package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UnusedLocalVariableInspection extends ElementInspection<QVarDeclaration> {
    public UnusedLocalVariableInspection() {
        super(QVarDeclaration.class);
    }

    @Override
    protected void validate(@NotNull QVarDeclaration variable, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final ElementContext context = QPsiUtil.getElementContext(variable);
        if (!context.is(ElementScope.LAMBDA)) {
            return;
        }

        if (QPsiUtil.isGlobalDeclaration(variable)) {
            return;
        }

        final QLambdaExpr lambda = context.lambda();

        // TODO: What about QSymbol?
        final Collection<QVarReference> variables = PsiTreeUtil.findChildrenOfType(lambda, QVarReference.class);
        if (variables.isEmpty()) {
            return;
        }

        final String qualifiedName = variable.getQualifiedName();
        for (QVarReference v : variables) {
            if (v.getQualifiedName().equals(qualifiedName)) {
                return;
            }
        }

        // Unused variable
        holder.registerProblem(variable, "Unused local variable `" + variable.getName() + "`", ProblemHighlightType.LIKE_UNUSED_SYMBOL, new LocalQuickFix() {
            @Override
            public @IntentionFamilyName
            @NotNull String getFamilyName() {
                return "Remove unused variable";
            }

            @Override
            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                final PsiElement v = descriptor.getPsiElement();
                if (isInsideCondition(v)) {
                    removeVariableOnly(v);
                } else {
                    removeWholeAssignment(v);
                }
            }
        });
    }

    private void removeVariableOnly(PsiElement variable) {
        final QAssignmentExpr assignment = (QAssignmentExpr) variable.getParent();
        final QExpression expression = assignment.getExpression();
        final List<PsiElement> elements = new ArrayList<>();
        PsiElement el = assignment.getFirstChild();
        while (el != null && el != expression) {
            elements.add(el);
            el = el.getNextSibling();
        }
        elements.forEach(PsiElement::delete);
    }

    private void removeWholeAssignment(PsiElement variable) {
        PsiElement assignment = variable.getParent();

        PsiElement parent = assignment.getParent();
        // Remove assignment is its only one in the expression
        if (parent instanceof QExpression && parent.getChildren().length == 1) {
            assignment = parent;
        }

        final List<PsiElement> rem = new ArrayList<>();
        rem.add(assignment);

        final PsiElement nextSibling = PsiTreeUtil.skipWhitespacesAndCommentsForward(assignment);
        if (nextSibling != null) {
            final IElementType elementType = nextSibling.getNode().getElementType();
            if (elementType == QTypes.SEMICOLON) {
                rem.add(nextSibling);
            }
        }
        rem.forEach(PsiElement::delete);
    }

    private boolean isInsideCondition(PsiElement var) {
        PsiElement e = var;
        PsiElement p = var.getParent();
        while (p != null && !(p instanceof QConditionExpr) && !(p instanceof QControlExpr)) {
            p = (e = p).getParent();
        }

        if (p instanceof QControlExpr c) {
            final List<QExpression> expressions = c.getExpressions();
            final int i = expressions.indexOf(e);
            return i == 0;
        }

        if (p instanceof QConditionExpr c) {
            final List<QExpression> expressions = c.getExpressions();
            final int i = expressions.indexOf(e);
            return i >= 0 && i < expressions.size() && (i % 2) == 0;
        }
        return false;
    }
}
