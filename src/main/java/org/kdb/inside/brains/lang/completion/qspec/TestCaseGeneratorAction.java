package org.kdb.inside.brains.lang.completion.qspec;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QPsiUtil;

import static org.kdb.inside.brains.psi.QPsiUtil.createLambda;
import static org.kdb.inside.brains.psi.QPsiUtil.moveCaret;

public class TestCaseGeneratorAction extends CodeInsightAction {
    private final TheCodeInsightActionHandler handler = new TheCodeInsightActionHandler();

    protected TestCaseGeneratorAction() {
        getTemplatePresentation().setText("QSpec TestCase");
        getTemplatePresentation().setDescription("Creates new emptyu QSpec" + TestDescriptor.SUITE + " test case");
    }

    @Override
    protected @NotNull CodeInsightActionHandler getHandler() {
        return handler;
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return file instanceof QFile;
    }

    private static class TheCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            final PsiElement code = createLambda(project, TestDescriptor.SUITE + "[\"\"]", false);

            final PsiElement psiElement;
            final PsiElement lastChild = file.getLastChild();
            if (lastChild == null) {
                psiElement = QPsiUtil.insert(project, code, file, false, false);
            } else {
                psiElement = QPsiUtil.insertAfter(project, code, lastChild, true);
            }

            if (psiElement != null) {
                moveCaret(editor, psiElement, TestDescriptor.SUITE.length() + 2);
            }
        }
    }
}
