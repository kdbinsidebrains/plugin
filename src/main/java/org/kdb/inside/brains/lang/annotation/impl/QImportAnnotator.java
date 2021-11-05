package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QImportCommand;

public class QImportAnnotator extends QElementAnnotator<QImportCommand> {
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
                    .withFix(new IntentionAction() {
                        @Override
                        public @IntentionName @NotNull String getText() {
                            return "Remove wrong spaces";
                        }

                        @Override
                        public @NotNull @IntentionFamilyName String getFamilyName() {
                            return "Q import command";
                        }

                        @Override
                        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                            return true;
                        }

                        @Override
                        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                            editor.getDocument().deleteString(t1 + 1, t2);
                        }

                        @Override
                        public boolean startInWriteAction() {
                            return true;
                        }
                    })
                    .create();
        }
    }
}
