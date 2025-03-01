package org.kdb.inside.brains.lang.inspection.fix;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.exclusions.UndefinedExclusion;
import org.kdb.inside.brains.lang.exclusions.UndefinedExclusionsService;
import org.kdb.inside.brains.psi.QVarReference;

public class ExcludeVariableFix extends LocalQuickFixOnPsiElement {
    final String variableName;

    public ExcludeVariableFix(@NotNull QVarReference variable) {
        super(variable);
        variableName = variable.getQualifiedName();
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Exclude variable";
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return "Add '" + variableName + "' to excluded list";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        UndefinedExclusionsService.getInstance().addExclusion(new UndefinedExclusion(variableName, false));
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
    }
}
