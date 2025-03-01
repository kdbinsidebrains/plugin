package org.kdb.inside.brains.lang.completion.qspec;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;
import org.kdb.inside.brains.psi.QExpressions;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QLambdaExpr;

import java.util.List;

import static org.kdb.inside.brains.psi.QPsiUtil.*;

// implementes GenerateActionPopupTemplateInjector - for templates?
public abstract class BaseQSpecGenerateAction extends CodeInsightAction {
    private final String functionName;
    private final String arguments;
    private final TheCodeInsightActionHandler handler = new TheCodeInsightActionHandler();

    public BaseQSpecGenerateAction(String functionName) {
        this(functionName, null);
    }

    protected BaseQSpecGenerateAction(String functionName, String arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
        getTemplatePresentation().setText("QSpec @" + functionName);
        getTemplatePresentation().setDescription("Generate empty @" + functionName + " QSpec function");
    }

    @Override
    protected @NotNull CodeInsightActionHandler getHandler() {
        return handler;
    }

    protected void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull TestDescriptor descriptor, @NotNull List<TestItem> items) {
        final PsiElement code = createLambda(project, functionName + (arguments != null ? arguments : ""), false);
        insertCode(project, editor, descriptor, code, arguments != null ? functionName.length() + 2 : -2, arguments == null);
    }

    protected void insertCode(@NotNull Project project,
                              @NotNull Editor editor,
                              @NotNull TestDescriptor descriptor,
                              @NotNull PsiElement code,
                              int caretOffset,
                              boolean first) {
        PsiElement psiElement = null;

        final TestItem root = descriptor.getLocalRoot();
        final QLambdaExpr lambda = root.getLambda();
        final QExpressions expressions = lambda.getExpressions();
        if (expressions == null) {
            PsiElement expr = ((QLambdaExpr) createCustomCode(project, "{`}")).getExpressions();
            if (expr != null) {
                expr = lambda.addAfter(clear(expr), lambda.getFirstChild());
                psiElement = insert(project, code, expr, true, false);
            }
        } else {
            if (first) {
                final PsiElement firstChild = expressions.getFirstChild();
                psiElement = insertBefore(project, code, firstChild, true);
            } else {
                final PsiElement lastChild = expressions.getLastChild();
                psiElement = insertAfter(project, code, lastChild, true);
            }
        }

        if (psiElement != null) {
            moveCaret(editor, psiElement, caretOffset);
        }
    }

    protected boolean isValidForTest(@NotNull Project project, @NotNull Editor editor, @NotNull TestDescriptor descriptor) {
        return true;
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof QFile)) {
            return false;
        }

        final TestDescriptor descriptor = getTestDescriptor(editor, file);
        if (descriptor == null) {
            return false;
        }
        return isValidForTest(project, editor, descriptor);
    }

    @Nullable
    protected TestDescriptor getTestDescriptor(Editor editor, PsiFile file) {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }
        return TestDescriptor.of(element);
    }

    private class TheCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            final TestDescriptor descriptor = getTestDescriptor(editor, file);
            if (descriptor != null) {
                BaseQSpecGenerateAction.this.invoke(project, editor, descriptor, descriptor.getLocalItems());
            }
        }
    }
}
