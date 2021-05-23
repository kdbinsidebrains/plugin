package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MixinLambdaVarDeclaration extends ElementInspection<QLambda> {
    public MixinLambdaVarDeclaration() {
        super(QLambda.class);
    }

    @Override
    protected void validate(@NotNull QLambda lambda, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final Collection<QVarDeclaration> declarations = PsiTreeUtil.findChildrenOfType(lambda, QVarDeclaration.class);
        if (declarations.isEmpty()) {
            return;
        }

        final Map<String, List<QVarDeclaration>> vars = declarations.stream().collect(Collectors.groupingBy(QVariable::getQualifiedName));
        for (Map.Entry<String, List<QVarDeclaration>> entry : vars.entrySet()) {
            final List<QVarDeclaration> value = entry.getValue();
            if (value.size() < 2) {
                continue;
            }

            boolean global = QPsiUtil.isGlobalDeclaration(value.get(0));
            for (QVarDeclaration declaration : value) {
                if (global != QPsiUtil.isGlobalDeclaration(declaration)) {
                    holder.registerProblem(declaration, "Initial declaration is " + (global ? "global" : "local") + " and mixin declaration causes an error", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix() {
                        @Override
                        public @IntentionFamilyName
                        @NotNull String getFamilyName() {
                            return "Change declaration to " + (global ? "global" : "local");
                        }

                        @Override
                        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                            final PsiElement colon = PsiTreeUtil.findSiblingForward(declaration, QTypes.COLON, true, null);
                            if (colon == null) {
                                return;
                            }
                            if (global) {
                                colon.getParent().addAfter(colon.copy(), colon);
                            } else {
                                colon.delete();
                            }
                        }
                    });
                }
            }
        }
    }
}
