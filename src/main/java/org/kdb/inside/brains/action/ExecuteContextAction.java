package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import static org.kdb.inside.brains.psi.QPsiUtil.findRootExpression;

public class ExecuteContextAction extends ExecuteAction {
    @Override
    protected TextRange getExecutionRange(Editor editor, DataContext context) {
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(context);
        if (file == null) {
            return null;
        }

        final PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        final PsiElement expr = findRootExpression(element);
        if (expr == null) {
            return null;
        }
        return expr.getTextRange();
    }
}