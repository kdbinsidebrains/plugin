package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QContext;

public class QContextAnnotator extends QElementAnnotator<QContext> {
    public QContextAnnotator() {
        super(QContext.class);
    }

    @Override
    public void annotate(@NotNull QContext context, @NotNull AnnotationHolder holder) {
        if (context.getVariable() == null) {
            return;
        }

        final String text = context.getText();
        final int length = text.length();

        int i = 2;
        int spaces = 0;
        for (i = 2; i < length; i++) {
            final char ch = text.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == 'f') {
                spaces++;
            } else {
                break;
            }
        }

        final TextRange textRange = context.getTextRange();
        if (spaces > 1) {
            final TextRange range = textRange.cutOut(new TextRange(2, i));
            holder.newAnnotation(HighlightSeverity.ERROR, "Only one space must separate command from the context name").range(range).withFix(new IntentionAction() {
                @Override
                public @IntentionName @NotNull String getText() {
                    return "Remove redundant spaces";
                }

                @Override
                public @NotNull @IntentionFamilyName String getFamilyName() {
                    return "Q context definition";
                }

                @Override
                public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                    return true;
                }

                @Override
                public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                    editor.getDocument().deleteString(range.getStartOffset() + 1, range.getEndOffset());
                }

                @Override
                public boolean startInWriteAction() {
                    return true;
                }
            }).create();
        }

        if (text.charAt(i) != '.') {
            final TextRange range = textRange.cutOut(new TextRange(i, text.length()));
            holder.newAnnotation(HighlightSeverity.ERROR, "Context must be started with a dot").range(range).withFix(new IntentionAction() {
                @Override
                public @IntentionName @NotNull String getText() {
                    return "Insert context prefix";
                }

                @Override
                public @NotNull @IntentionFamilyName String getFamilyName() {
                    return "Q context definition";
                }

                @Override
                public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                    return true;
                }

                @Override
                public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                    editor.getDocument().insertString(range.getStartOffset(), ".");
                }

                @Override
                public boolean startInWriteAction() {
                    return true;
                }
            }).create();
        }
    }
}
