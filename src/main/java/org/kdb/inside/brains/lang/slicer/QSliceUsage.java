package org.kdb.inside.brains.lang.slicer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.slicer.SliceAnalysisParams;
import com.intellij.slicer.SliceUsage;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;
import java.util.Objects;

public class QSliceUsage extends SliceUsage {
    private final boolean recursion;

    public QSliceUsage(@NotNull PsiElement element, @NotNull SliceAnalysisParams params) {
        super(element, params);
        recursion = recursion(element);
    }

    public QSliceUsage(@NotNull QVarDeclaration element, @NotNull SliceUsage parent, @NotNull SliceAnalysisParams params) {
        super(element, parent, params);
        recursion = recursion(element);
    }

    public boolean isRecursion() {
        return recursion;
    }

    @Override
    protected @NotNull SliceUsage copy() {
        return new QSliceUsage((QVarDeclaration) Objects.requireNonNull(getElement()), getParent(), params);
    }

    @Override
    protected void processUsagesFlownFromThe(PsiElement element, Processor<? super SliceUsage> uniqueProcessor) {
        if (recursion) {
            return;
        }

        if (!(element instanceof QVarDeclaration)) {
            return;
        }

        final PsiElement parent = element.getParent();
        if (!(parent instanceof QAssignmentExpr exp)) {
            return;
        }

        if (!(exp.getExpression() instanceof QLambdaExpr)) {
            return;
        }

        final Collection<QPsiElement> children = PsiTreeUtil.findChildrenOfAnyType(exp, QVarReference.class, QSymbol.class);
        for (QPsiElement child : children) {
            final PsiReference reference = child.getReference();
            if (reference == null) {
                continue;
            }
            final PsiElement el = reference.resolve();
            if (!(el instanceof QVarDeclaration d) || (params.valueFilter != null && !params.valueFilter.allowed(el)) || !params.scope.contains(el)) {
                continue;
            }
            if (QPsiUtil.isGlobalDeclaration(d)) {
                uniqueProcessor.process(new QSliceUsage(d, QSliceUsage.this, params));
            }
        }
    }

    private boolean recursion(PsiElement d) {
        SliceUsage e = this.getParent();
        while (e != null) {
            if (d.equals(e.getElement())) {
                return true;
            }
            e = e.getParent();
        }
        return false;
    }

    @Override
    protected void processUsagesFlownDownTo(PsiElement element, Processor<? super SliceUsage> uniqueProcessor) {
    }
}
