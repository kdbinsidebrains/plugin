package org.kdb.inside.brains.lang.refactoring;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class QIntroduceVariableHandler implements RefactoringActionHandler {
    private final VarType varType;

    public QIntroduceVariableHandler(VarType varType) {
        this.varType = varType;
    }

    private static void showErrorMessage(Project project, Editor editor) {
        CommonRefactoringUtil.showErrorHint(project, editor, "Selected block should represent an expression", RefactoringBundle.message("introduce.parameter.title"), HelpID.INTRODUCE_PARAMETER);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        if (!(file instanceof QFile)) {
            return;
        }

        final SelectionModel selection = editor.getSelectionModel();
        final int start = selection.getSelectionStart();
        final int end = selection.getSelectionEnd();
        // Only single selection is supported
        if (start < 0 || end < 0) {
            return;
        }

        // Just cursor position is not supported. User-case is not clear for now
        if (start == end) {
            showErrorMessage(project, editor);
            return;
        }

        final PsiElement element = file.findElementAt(start);
        if (element == null || element instanceof PsiWhiteSpace || element.getTextOffset() != start) {
            showErrorMessage(project, editor);
            return;
        }

        final PsiElement endEl = file.findElementAt(end - 1);
        if (endEl == null || endEl instanceof PsiWhiteSpace || (endEl.getTextOffset() + endEl.getTextLength()) != end) {
            showErrorMessage(project, editor);
            return;
        }

        final PsiElement anchor = getAnchor(file, element);
        if (anchor == null) {
            showErrorMessage(project, editor);
            return;
        }

        final PsiElement context = anchor.getParent();

        final LinkedHashSet<String> suggestedNames = getSuggestedNames(element, context);

        final String varName = suggestedNames.iterator().next();
        final PsiElement newVar = WriteCommandAction.writeCommandAction(project).withName("Introduce Local Variable").compute(() -> {
            final String indent = getIndent(anchor);

            final Document document = editor.getDocument();

            String text = document.getText(new TextRange(start, end));

            final String postfix = isEndSpaceRequired(file, end) ? " " : "";
            if (text.endsWith(";")) {
                document.replaceString(start, end, varName + ";" + postfix);
            } else {
                text = text + ";";
                document.replaceString(start, end, varName + postfix);
            }

            final int offset = anchor.getTextOffset();
            document.insertString(offset, varName + ":" + text + "\n" + indent);

            PsiDocumentManager.getInstance(project).commitDocument(document);

            editor.getCaretModel().moveToOffset(offset);
            return file.findElementAt(offset);
        });

        final PsiElement variable = newVar.getParent();
        if (variable instanceof QVarDeclaration) {
            editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            new QInplaceVariableIntroducer((QVarDeclaration) variable, editor, project).performInplaceRefactoring(suggestedNames);
        }
    }

    private boolean isEndSpaceRequired(PsiFile file, int end) {
        final PsiElement tail = file.findElementAt(end);
        if (tail == null) {
            return false;
        }
        if (tail instanceof PsiWhiteSpace) {
            return false;
        }
        if (tail instanceof LeafPsiElement) {
            final String text = tail.getText();
            return !text.equals(";") && !text.equals("[") && !text.equals("(") && !text.equals("{");
        }
        return true;
    }

    private LinkedHashSet<String> getSuggestedNames(PsiElement element, PsiElement context) {
        final PsiElement parent = element.getParent();
        final String name = parent instanceof QVariable ? ((QVariable) parent).getQualifiedName().toLowerCase() : "v";

        final Set<String> existNames = PsiTreeUtil.getChildrenOfAnyType(context, QAssignmentExpr.class).stream().map(QAssignmentExpr::getVarDeclaration).filter(Objects::nonNull).map(QVariable::getQualifiedName).collect(Collectors.toSet());

        final LinkedHashSet<String> res = new LinkedHashSet<>();
        int i = name.lastIndexOf('.');
        while (i > 0) {
            String v = name.substring(i + 1).replace(".", "");
            res.add(fixName(v, existNames));
            i = name.lastIndexOf('.', i - 1);
        }
        res.add(fixName(name.replace(".", ""), existNames));
        return res;
    }

    private String fixName(String name, Set<String> exist) {
        if (!exist.contains(name)) {
            return name;
        }

        int i = 1;
        String res = name + i;
        while (exist.contains(res)) {
            res = name + (++i);
        }
        return res;
    }

    private PsiElement getAnchor(PsiFile file, PsiElement element) {
        if (varType == VarType.VARIABLE) {
            return getViableAnchor(element);
        }
        return getFieldAnchor(file);
    }

    private PsiElement getViableAnchor(PsiElement element) {
        PsiElement cur = element;
        PsiElement parent = element.getParent();
        while (parent != null && !(parent instanceof PsiFile || parent instanceof QExpressions)) {
            cur = parent;
            parent = parent.getParent();
        }
        return cur;
    }

    private PsiElement getFieldAnchor(PsiFile file) {
        PsiElement e = file.getFirstChild();
        while (e instanceof PsiWhiteSpace || e instanceof QImport || e instanceof QCommand) {
            e = e.getNextSibling();
        }
        return e == null ? file.getFirstChild() : e;
    }

    private String getIndent(PsiElement context) {
        PsiElement prevSibling = context.getPrevSibling();
        if (prevSibling == null && context.getParent() != null) {
            prevSibling = context.getParent().getPrevSibling();
        }

        if (!(prevSibling instanceof PsiWhiteSpace)) {
            return "";
        }

        final String text = prevSibling.getText();
        final int i = text.lastIndexOf('\n');
        return i < 0 ? text : text.substring(i + 1);
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {
        // Found no use-case for this method. Probably make sense only for Java language
    }

    public enum VarType {
        VARIABLE, CONSTANT, FIELD
    }
}
