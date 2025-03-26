package org.kdb.inside.brains.lang.annotation;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class BaseIntentionAction extends AbstractIntentionAction {
    private final String text;
    private final String familyName;
    private final IntentionInvoke invoke;

    public BaseIntentionAction(@IntentionFamilyName @NotNull String text, @NotNull IntentionInvoke invoke) {
        this(text, text, invoke);
    }

    public BaseIntentionAction(@IntentionFamilyName @NotNull String familyName, @IntentionName @NotNull String text, @NotNull IntentionInvoke invoke) {
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
    public final void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        invoke.invoke(project, editor, file);
    }
}