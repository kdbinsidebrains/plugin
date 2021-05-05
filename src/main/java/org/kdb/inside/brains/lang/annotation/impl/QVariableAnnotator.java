package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.QPsiUtil;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;

public class QVariableAnnotator extends QElementAnnotator<QVariable> {
    public QVariableAnnotator() {
        super(QVariable.class);
    }

    @Override
    public void annotate(@NotNull QVariable variable, @NotNull AnnotationHolder holder) {
        if (variable.isDeclaration()) {
            validateUnused(variable, holder);
        } else {
            validateDeclaration(variable, holder);
        }
    }

    private void validateDeclaration(QVariable variable, AnnotationHolder holder) {
        final String variableName = variable.getQualifiedName();
        if (QLanguage.isKeyword(variableName) || QLanguage.isSystemFunction(variableName)) {
            return;
        }

        if (QPsiUtil.isImplicitVariable(variable)) {
            return;
        }

        if (variable.getContext(QQuery.class) != null) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        if (variable.getContext(QTable.class) != null) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        final PsiReference reference = variable.getReference();
        if (reference instanceof PsiPolyVariantReference) {
            final ResolveResult[] resolveResults = ((PsiPolyVariantReference) reference).multiResolve(false);
            if (resolveResults.length != 0) {
                return;
            }
        }
        holder.newAnnotation(HighlightSeverity.WARNING, "`" + variableName + "` might not have been defined").textAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES).range(variable).create();
    }

    private void validateUnused(QVariable variable, AnnotationHolder holder) {
        // ignore all columns
        if (variable.getContext(QColumnAssignment.class) != null) {
            return;
        }

        final String qualifiedName = variable.getQualifiedName();

        final QLambda lambda = variable.getContext(QLambda.class);
        if (lambda == null) { // global variable
/*
            final AtomicInteger i = new AtomicInteger();

            final Project project = variable.getProject();
            final QIndexService instance = QIndexService.getInstance(project);
            instance.processAllKeys(s -> {
                if (s.equals(qualifiedName)) {
                    i.incrementAndGet();
                }
                return true;
            }, GlobalSearchScope.allScope(project));

            if (i.get() <= 1) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Unused global variable `" + variable.getName() + "'").range(variable).textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES).create();
            }
*/
        } else {
            // TODO: What about QSymbol?
            final Collection<QVariable> children = PsiTreeUtil.findChildrenOfType(lambda, QVariable.class);
            if (children.isEmpty()) {
                return;
            }

            for (QVariable v : children) {
                if (v == variable) {
                    continue;
                }
                if (v.getQualifiedName().equals(qualifiedName) && !v.isDeclaration()) {
                    return;
                }
            }
            // Unused variable
            holder.newAnnotation(HighlightSeverity.WARNING, "Unused local variable `" + variable.getName() + "'").range(variable).textAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES).create();
        }
    }
}
