package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UnusedLocalVariableInspection extends ElementInspection<QVariable> {
    public UnusedLocalVariableInspection() {
        super(QVariable.class);
    }

    @Override
    protected void validate(@NotNull QVariable variable, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final QLambda lambda = variable.getContext(QLambda.class);
        if (lambda == null) {
            return;
        }

        final AssignmentType type = QPsiUtil.getAssignmentType(variable);
        if (type != AssignmentType.LOCAL) {
            return;
        }

        // TODO: What about QSymbol?
        final Collection<QVariable> variables = PsiTreeUtil.findChildrenOfType(lambda, QVariable.class);
        if (variables.isEmpty()) {
            return;
        }

        final String qualifiedName = variable.getQualifiedName();
        for (QVariable v : variables) {
            if (v == variable) {
                continue;
            }

            if (v.getQualifiedName().equals(qualifiedName) && QPsiUtil.getAssignmentType(v) == null) {
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

                List<PsiElement> rem = new ArrayList<>();
                rem.add(removing);

                PsiElement nextSibling = removing.getNextSibling();
                while (nextSibling instanceof PsiWhiteSpace) {
                    nextSibling = nextSibling.getNextSibling();
                }

                if (nextSibling.getNode().getElementType() == QTypes.SEMICOLON) {
                    rem.add(nextSibling);
                }

                rem.forEach(PsiElement::delete);
            }
        });
    }
}
