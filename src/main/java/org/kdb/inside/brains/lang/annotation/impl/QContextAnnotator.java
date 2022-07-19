package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.lang.annotation.WriteIntentionAction;
import org.kdb.inside.brains.psi.QContext;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;

public class QContextAnnotator extends QElementAnnotator<QContext> {
    private static final String FAMILY_NAME = "Q context definition";

    public QContextAnnotator() {
        super(QContext.class);
    }

    @Override
    public void annotate(@NotNull QContext context, @NotNull AnnotationHolder holder) {
        final QVarDeclaration variable = context.getVariable();
        if (variable == null) {
            return;
        }

        checkSpaces(context, holder);

        final String qualifiedName = variable.getQualifiedName();
        if (qualifiedName.isEmpty()) {
            return;
        }

        checkLeadingDot(variable, holder, qualifiedName);

        checkContextDepth(variable, holder, qualifiedName);
    }

    private void checkLeadingDot(QVarDeclaration variable, AnnotationHolder holder, String qualifiedName) {
        if (qualifiedName.charAt(0) == '.') {
            return;
        }

        final TextRange range = variable.getTextRange();
        holder.newAnnotation(HighlightSeverity.ERROR, "Context must be started with a dot")
                .range(range)
                .withFix(new WriteIntentionAction(FAMILY_NAME, "Insert context prefix", (p, e, f) -> variable.replace(QPsiUtil.createVarDeclaration(p, "." + qualifiedName))))
                .create();
    }

    private void checkContextDepth(QVarDeclaration variable, AnnotationHolder holder, String qualifiedName) {
        final int i = qualifiedName.indexOf('.', 1);
        if (i < 0) {
            return;
        }

        final TextRange range = variable.getTextRange();
        holder.newAnnotation(HighlightSeverity.ERROR, "Context must be one level depth")
                .range(range)
                .withFix(new WriteIntentionAction(FAMILY_NAME, "Remove all invalid context levels", (p, e, f) -> variable.replace(QPsiUtil.createVarDeclaration(p, qualifiedName.substring(0, i)))))
                .create();
    }

    private void checkSpaces(@NotNull QContext context, @NotNull AnnotationHolder holder) {
        final String text = context.getText();
        final int spaces = getLeadingSpaces(text);
        final TextRange textRange = context.getTextRange();
        if (spaces <= 1) {
            return;
        }

        final TextRange range = textRange.cutOut(new TextRange(2, 2 + spaces));
        holder.newAnnotation(HighlightSeverity.ERROR, "Only one space must separate command from the context name")
                .range(range)
                .withFix(new WriteIntentionAction(FAMILY_NAME, "Remove redundant spaces", (p, e, f) -> e.getDocument().deleteString(range.getStartOffset() + 1, range.getEndOffset())))
                .create();
    }

    private int getLeadingSpaces(String text) {
        final int length = text.length();
        for (int i = 2; i < length; i++) {
            final char ch = text.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return i - 2;
            }
        }
        return length - 2;
    }
}
