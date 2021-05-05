package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class QTypedHandler extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (c == '\\') {
            final CaretModel caretModel = editor.getCaretModel();

            final int offset = caretModel.getOffset();
            final Document document = editor.getDocument();

            final int lineNumber = document.getLineNumber(offset);
            final int lineStart = document.getLineStartOffset(lineNumber);

            // First and only one slash in the line - that's system command
            final String text = document.getText(new TextRange(lineStart, offset)).strip();
            if (text.length() == 1 && text.charAt(0) == '\\') {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
                return Result.STOP;
            }

/*
            final PsiElement element = file.findElementAt(offset);
            if (element == null) {
                System.out.println("No element???");
                return Result.CONTINUE;
            }

            System.out.println("Found element: " + element.toString());
*/
        }
/*
        if (c == ' ' || c == '/' || c == '\') {
    }
*/
/*
        if (c == ' ' || c == '/') {
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = file.findElementAt(offset);
            while (element == null && offset > 0) {
                offset--;
                element = file.findElementAt(offset);
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        }
*/
/*

            if (element instanceof QImportElement) {
                System.out.println("asdasd");
                return Result.CONTINUE;
            }
*//*

        }
*/

        // TODO: not implemented yet
        return Result.CONTINUE;
/*
        if (!(file instanceof QFile)) {
            return Result.CONTINUE;
        }
        if (isInsideStringLiteral(editor, file)) {
            return Result.CONTINUE;
        }
        if (c == '[' || c == ';') {
            AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null);
        }
        return Result.CONTINUE;
*/
    }

    private static boolean isInsideStringLiteral(@NotNull Editor editor, @NotNull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return false;
        }
        final ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(element.getLanguage());
        if (definition != null) {
            final TokenSet stringLiteralElements = definition.getStringLiteralElements();
            final ASTNode node = element.getNode();
            if (node == null) {
                return false;
            }
            final IElementType elementType = node.getElementType();
            if (stringLiteralElements.contains(elementType)) {
                return true;
            }
            PsiElement parent = element.getParent();
            if (parent != null) {
                ASTNode parentNode = parent.getNode();
                return parentNode != null && stringLiteralElements.contains(parentNode.getElementType());
            }
        }
        return false;
    }
}
