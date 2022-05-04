package org.kdb.inside.brains.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

public class ExecuteContextAction extends ExecuteAction {
    public ExecuteContextAction() {
    }

    @Override
    protected TextRange getExecutionRange(PsiFile file, Editor editor) {
        return findExpressionRange(getElement(file, editor));
    }

    @Nullable
    private PsiElement getElement(PsiFile file, Editor editor) {
        return file.findElementAt(editor.getCaretModel().getOffset());
    }

    private TextRange findExpressionRange(PsiElement element) {
        if (element == null) {
            return null;
        }

        PsiElement cur = element;
        PsiElement prev = element.getParent();
        while (prev != null && !(prev instanceof PsiFile)) {
            cur = prev;
            prev = prev.getParent();
        }
        return cur.getTextRange();
    }
}