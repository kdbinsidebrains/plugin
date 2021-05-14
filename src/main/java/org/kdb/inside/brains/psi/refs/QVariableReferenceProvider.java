package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QPsiUtil;
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
        final QVariable var = (QVariable) element;
/*
        final QVariable localDef = findFirstLocalDefinition(var);
        if (var == localDef) {
            return PsiReference.EMPTY_ARRAY;
        }
*/
        return new PsiReference[]{new QVariableReference(var)};
    }

    private static QVariable findFirstLocalDefinition(final QVariable variable) {
        final QLambda lambda = variable.getContext(QLambda.class);
        if (lambda == null) {
            return null;
        }

        final String qualifiedName = variable.getQualifiedName();
        final Collection<QVariable> lambdaVars = PsiTreeUtil.findChildrenOfType(lambda, QVariable.class);
        return lambdaVars.stream().filter(var -> var.getQualifiedName().equals(qualifiedName) && QPsiUtil.getAssignmentType(var) != null).findFirst().orElse(null);
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
            final String qualifiedName = myElement.getQualifiedName();

            final QVariable localDef = findFirstLocalDefinition(myElement);
            if (localDef != null) {
/*
                if (localDef == myElement) {
                    return ResolveResult.EMPTY_ARRAY;
                }
*/
                return new ResolveResult[]{new PsiElementResolveResult(localDef)};
            }

/*

            final QLambda lambda = myElement.getContext(QLambda.class);
            if (lambda != null) {
                if (QPsiUtil.isImplicitVariable(myElement)) {
                    return ResolveResult.EMPTY_ARRAY;
                }

                final Collection<QVariable> lambdaVars = PsiTreeUtil.findChildrenOfType(lambda, QVariable.class);

                final Optional<QVariable> first = lambdaVars.stream().filter(var -> var.getQualifiedName().equals(qualifiedName) && QPsiUtil.getAssignmentType(var) != null).findFirst();
                if (first.isPresent()) {
                    final QVariable variable = first.get();
*/
/*
                    if (variable == myElement) {
                        return ResolveResult.EMPTY_ARRAY;
                    } else {
*//*

                    return new ResolveResult[]{new PsiElementResolveResult(variable)};
//                    }
                }
            }
*/

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
