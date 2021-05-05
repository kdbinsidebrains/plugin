package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInspection.*;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QImportFile;

public class QImportAnnotator extends QElementAnnotator<QImportFile> {
    public QImportAnnotator() {
        super(QImportFile.class);
    }

    @Override
    public void annotate(@NotNull QImportFile element, @NotNull AnnotationHolder holder) {
        checkReferences(element.getReferences(), holder);
    }

    private void checkReferences(PsiReference[] references, AnnotationHolder holder) {
        for (PsiReference reference : references) {
            if (reference.isSoft()) {
                continue;
            }
            if (reference.resolve() != null) {
                continue;
            }

            if (reference instanceof PsiPolyVariantReference) {
                final PsiPolyVariantReference pvr = (PsiPolyVariantReference) reference;
                if (pvr.multiResolve(false).length == 0) {
                    addError(reference, holder);
                }
            } else {
                addError(reference, holder);
            }
        }
    }

    private void addError(PsiReference reference, AnnotationHolder holder) {
        final TextRange rangeInElement = reference.getRangeInElement();
        final TextRange range = TextRange.from(reference.getElement().getTextRange().getStartOffset()
                + rangeInElement.getStartOffset(), rangeInElement.getLength());

        String message;
        if (reference instanceof EmptyResolveMessageProvider) {
            message = ((EmptyResolveMessageProvider) reference).getUnresolvedMessagePattern();
        } else {
            message = "Cannot resolve file '" + reference.getCanonicalText() + '\'';
        }

        AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, message).range(range)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);

        if (reference instanceof LocalQuickFixProvider) {
            LocalQuickFix[] fixes = ((LocalQuickFixProvider) reference).getQuickFixes();
            if (fixes != null) {
                InspectionManager inspectionManager = InspectionManager.getInstance(reference.getElement().getProject());
                for (LocalQuickFix fix : fixes) {
                    ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(reference.getElement(), message, fix,
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, true);
                    builder = builder.newLocalQuickFix(fix, descriptor).registerFix();
                }
            }
        }
        builder.create();
    }
}
