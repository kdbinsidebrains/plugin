package org.kdb.inside.brains.lang.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * <a href="https://github.com/JetBrains/intellij-community/tree/idea/223.7571.182/python/src/com/jetbrains/python/codeInsight/postfix">Examples</a>
 */
public class KdbPostfixTemplateProvider implements PostfixTemplateProvider {
    @Override
    public @NotNull @NonNls String getId() {
        return "kdb+q";
    }

    @Override
    public @NotNull Set<PostfixTemplate> getTemplates() {
        return Set.of();
    }

    @Override
    public boolean isTerminalSymbol(char currentChar) {
        return false;
    }

    @Override
    public void preExpand(@NotNull PsiFile file, @NotNull Editor editor) {

    }

    @Override
    public void afterExpand(@NotNull PsiFile file, @NotNull Editor editor) {

    }

    @Override
    public @NotNull PsiFile preCheck(@NotNull PsiFile copyFile, @NotNull Editor realEditor, int currentOffset) {
        return copyFile;
    }
}
