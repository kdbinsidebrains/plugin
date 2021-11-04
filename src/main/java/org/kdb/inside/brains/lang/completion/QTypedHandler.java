package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;

public class QTypedHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (charTyped == '[') {
            AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null);
            return Result.STOP;
        }

        final boolean slash = charTyped == '/';
        final boolean space = charTyped == ' ';
        if (slash || space) {
            final int offset = editor.getCaretModel().getOffset();
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                final PsiElement leaf = f.findElementAt(offset);
                if (leaf == null) {
                    return false;
                }

                QImport qImport = null;
                if (leaf.getParent() instanceof QImport) {
                    qImport = (QImport) leaf.getParent();
                } else if (leaf.getPrevSibling() instanceof QImport) {
                    qImport = (QImport) leaf.getPrevSibling();
                }

                return qImport != null && (slash || qImport.getFilePathRange().isEmpty());
            });
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
