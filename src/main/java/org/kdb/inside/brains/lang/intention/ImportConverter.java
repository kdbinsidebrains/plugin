package org.kdb.inside.brains.lang.intention;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;

public class ImportConverter extends BaseElementAtCaretIntentionAction implements IntentionAction {
    @Override
    public @IntentionName @NotNull String getText() {
        return "Converts system \"\\l ... \" to \\l ... and vise versa";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "ImportConvertIntention";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        System.out.println("Invoke");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (element.getParent() instanceof QImport qImport) {
            System.out.println("asdasd");
            return true;
        }
        return false;
    }
}
