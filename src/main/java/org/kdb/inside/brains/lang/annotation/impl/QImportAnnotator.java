package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.lang.annotation.WriteIntentionAction;
import org.kdb.inside.brains.psi.QImportCommand;

public class QImportAnnotator extends QElementAnnotator<QImportCommand> {
    private static final String FAMILY_NAME = "Importing";

    public QImportAnnotator() {
        super(QImportCommand.class);
    }

    @Override
    protected void annotate(@NotNull QImportCommand element, @NotNull AnnotationHolder holder) {
        final PsiElement firstChild = element.getFirstChild();
        final PsiElement lastChild = element.getLastChild();

        final int t1 = firstChild.getTextOffset() + firstChild.getTextLength();
        final int t2 = lastChild.getTextOffset();

        if (t2 - t1 > 1) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Only one space is allowed in import command")
                    .range(new TextRange(t1, t2))
                    .withFix(new WriteIntentionAction("Remove wrong spaces", FAMILY_NAME, (p, e, f) -> e.getDocument().deleteString(t1 + 1, t2)))
                    .create();
        }
    }
}
