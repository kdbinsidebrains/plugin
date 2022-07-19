package org.kdb.inside.brains.lang.annotation;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class WriteIntentionAction extends AbstractIntentionAction {
    private final String text;
    private final String familyName;
    private final Invoke invoke;

    public WriteIntentionAction(String text, Invoke invoke) {
        this(text, text, invoke);
    }

    public WriteIntentionAction(String familyName, String text, Invoke invoke) {
        this.text = text;
        this.familyName = familyName;
        this.invoke = invoke;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return text;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return familyName;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        invoke.invoke(project, editor, file);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @FunctionalInterface
    public interface Invoke {
        void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException;
    }
}