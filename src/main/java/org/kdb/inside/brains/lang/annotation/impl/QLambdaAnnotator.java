package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QParameters;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.QVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class QLambdaAnnotator extends QElementAnnotator<QParameters> {
    public QLambdaAnnotator() {
        super(QParameters.class);
    }

    @Override
    public void annotate(@NotNull QParameters element, @NotNull AnnotationHolder holder) {
        final List<QVarDeclaration> params = element.getVariables();
        if (params.size() > 8) {
            for (int i = 8; i < params.size(); i++) {
                final QVariable var = params.get(i);
                holder.newAnnotation(HighlightSeverity.ERROR, "Only 8 parameters are allowed.")
                        .range(var)
                        .withFix(new AbstractIntentionAction() {
                            @Override
                            public @IntentionName @NotNull String getText() {
                                return "Remove parameter";
                            }

                            @Override
                            public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                                final Document document = editor.getDocument();

                                final String text = element.getText();
                                final TextRange pr = var.getTextRangeInParent();

                                final int start = text.lastIndexOf(';', pr.getStartOffset());

                                int end = text.indexOf(';', pr.getEndOffset());
                                if (end == -1) {
                                    end = text.indexOf(']', pr.getEndOffset());
                                }

                                final int off = element.getTextOffset();
                                final int gs = off + start;
                                final int ge = off + end;
                                ApplicationManager.getApplication().runWriteAction(() -> document.deleteString(gs, ge));
                            }
                        })
                        .create();
            }
        }

        final Map<String, QVariable> names = new HashMap<>();
        for (QVariable param : params) {
            final QVariable previous = names.put(param.getName(), param);
            if (previous != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate parameter name.").range(param).withFix(new RenameIntentionAction(param)).create();
                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate parameter name.").range(previous).withFix(new RenameIntentionAction(previous)).create();
            }
        }
    }

    private static class RenameIntentionAction extends AbstractIntentionAction {
        private final QVariable variable;

        private RenameIntentionAction(QVariable variable) {
            this.variable = variable;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return "Rename parameter";
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final TextRange r = variable.getTextRange();
            editor.getSelectionModel().setSelection(r.getStartOffset(), r.getEndOffset());
        }
    }
}
