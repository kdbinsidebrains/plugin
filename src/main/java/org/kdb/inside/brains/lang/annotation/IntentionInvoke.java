package org.kdb.inside.brains.lang.annotation;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IntentionInvoke {
    void invoke(@NotNull Project project, @NotNull Editor editor, PsiFile file) throws IncorrectOperationException;
}
