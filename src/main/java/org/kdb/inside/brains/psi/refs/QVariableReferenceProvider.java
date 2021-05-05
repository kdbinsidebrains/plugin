package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QPsiUtil;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QVariable;
import org.kdb.inside.brains.psi.index.QIndexService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QVariableReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof QVariable)) {
            return PsiReference.EMPTY_ARRAY;
        }
        return new PsiReference[]{new QVariableReference((QVariable) element)};
    }

    public static class QVariableReference extends PsiPolyVariantReferenceBase<QVariable> {
        public QVariableReference(@NotNull QVariable element) {
            super(element);
        }

        @Override
        public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
            return myElement.setName(newElementName);
        }

        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
            final int stopOffset = myElement.getTextOffset();
            final String qualifiedName = myElement.getQualifiedName();

            final QLambda lambda = myElement.getContext(QLambda.class);
            if (lambda != null) {
                if (QPsiUtil.isImplicitVariable(myElement)) {
                    return ResolveResult.EMPTY_ARRAY;
                }

                final Collection<QVariable> lambdaVars = PsiTreeUtil.findChildrenOfType(lambda, QVariable.class);
                for (QVariable var : lambdaVars) {
                    if (var == myElement) {
                        continue;
                    }
                    if (var.getTextOffset() >= stopOffset) {
                        break;
                    }
                    if (var.getQualifiedName().equals(qualifiedName) && var.isDeclaration()) {
                        return new ResolveResult[]{new PsiElementResolveResult(var)};
                    }
                }
            }

            final Project project = myElement.getProject();

            final QIndexService index = QIndexService.getInstance(project);
            final List<PsiElementResolveResult> elements = new ArrayList<>();
            index.processVariables(s -> s.equals(qualifiedName), GlobalSearchScope.allScope(project), (key, file, descriptor, variable) -> {
                if (variable != myElement) {
                    elements.add(new PsiElementResolveResult(variable, true));
                }
            });
            return elements.toArray(ResolveResult[]::new);
        }
    }
}
