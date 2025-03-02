package org.kdb.inside.brains.lang.inspection.fix;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.qspec.QSpecConfigurable;
import org.kdb.inside.brains.psi.QVarReference;

public class QSpecLibraryFix extends LocalQuickFixOnPsiElement {
    public QSpecLibraryFix(QVarReference reference) {
        super(reference);
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return "Setup QSpec testing framework";
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "QSpec testing framework";
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        QSpecConfigurable.showConfigurable(project);
    }
}
