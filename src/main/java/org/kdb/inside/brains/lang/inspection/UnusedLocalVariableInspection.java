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
                PsiElement removing = descriptor.getPsiElement().getParent();
                PsiElement parent = removing.getParent();
                if (parent instanceof QExpression && parent.getChildren().length == 1) {
                    removing = parent;
                }

                final List<PsiElement> rem = new ArrayList<>();
                rem.add(removing);

                final PsiElement nextSibling = PsiTreeUtil.skipWhitespacesAndCommentsForward(removing);
                if (nextSibling != null) {
                    final IElementType elementType = nextSibling.getNode().getElementType();
                    if (elementType == QTypes.SEMICOLON) {
                        rem.add(nextSibling);
                    }
                }
                rem.forEach(PsiElement::delete);
            }
        });
    }
}
