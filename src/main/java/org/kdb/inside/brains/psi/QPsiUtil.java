package org.kdb.inside.brains.psi;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiEditorUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class QPsiUtil {
    private QPsiUtil() {
    }

    public static String getLocationString(QPsiElement element) {
        final PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? "" : containingFile.getName();
    }

    public static boolean isResolvableReference(PsiReference reference) {
        if (reference instanceof PsiPolyVariantReference poly) {
            final ResolveResult[] resolveResults = poly.multiResolve(false);
            for (ResolveResult resolveResult : resolveResults) {
                if (resolveResult.isValidResult() && resolveResult.getElement() != null) {
                    return true;
                }
            }
        } else {
            return reference.resolve() != null;
        }
        return false;
    }

    public static boolean hasResolvedReference(PsiElement element) {
        for (PsiReference reference : element.getReferences()) {
            if (isResolvableReference(reference)) {
                return true;
            }
        }
        return false;
    }

    public static String getTypeCast(@NotNull QTypeCastExpr cast) {
        final String text = cast.getTypeCast().getText();
        final String name = text.charAt(0) == '`' ? text.substring(1, text.length() - 1) : text.substring(1, text.length() - 2);
        if (name.isEmpty()) {
            return "symbol";
        }
        return name;
    }

    public static boolean hasNamespace(@NotNull String identifier) {
        return !identifier.isEmpty() && identifier.charAt(0) == '.';
    }

    public static boolean isImplicitName(@NotNull String name) {
        if (name.length() != 1) {
            return false;
        }
        final char c = name.charAt(0);
        return 'x' == c || 'y' == c || 'z' == c;
    }

    public static boolean isImplicitVariable(@NotNull QVariable variable) {
        if (isImplicitName(variable.getQualifiedName())) {
            final QLambdaExpr enclosingLambda = variable.getContext(QLambdaExpr.class);
            return enclosingLambda != null && enclosingLambda.getParameters() == null;
        }
        return false;
    }

    public static boolean isGlobalDeclaration(@NotNull QVarDeclaration declaration) {
        final ElementContext ctx = ElementContext.of(declaration);
        return switch (ctx.getScope()) {
            case FILE, CONTEXT -> true;
            case DICT, TABLE, QUERY, PARAMETERS -> false;
            case LAMBDA -> {
                if (hasNamespace(declaration.getQualifiedName())) {
                    yield true;
                }

                final PsiElement parent = declaration.getParent();
                QAssignmentType assignmentType = null;
                if (parent instanceof QAssignmentExpr a) {
                    assignmentType = a.getAssignmentType();
                } else if (parent instanceof QTypedVariable tv && tv.getParent() instanceof QPatternDeclaration pd && pd.getParent() instanceof QAssignmentExpr a) {
                    assignmentType = a.getAssignmentType();
                }
                yield assignmentType != null && "::".equals(assignmentType.getText());
            }
        };
    }

    /**
     * Checks is the specified element colon or not.
     *
     * @param el the element to be checked
     * @return <code>true</code> if the element is colon; <code>false</code> - otherwise.
     */
    public static boolean isColon(PsiElement el) {
        return isLeafText(el, ":");
    }

    public static boolean isSemicolon(PsiElement el) {
        return isLeafText(el, ";");
    }

    public static boolean isWhitespace(PsiElement el) {
        return el instanceof PsiWhiteSpace || el instanceof PsiComment;
    }

    public static boolean isLeafText(PsiElement el, String text) {
        return el instanceof LeafPsiElement && text.equals(el.getText());
    }

    public static boolean isLeafText(PsiElement el, Predicate<String> predicate) {
        return el instanceof LeafPsiElement && predicate.test(el.getText());
    }

    /**
     * Returns the appropriate VarAssignment object if the specified element is an expression in an assignment expression
     */
    public static @Nullable VarAssignment getVarAssignment(@NotNull QPsiElement element) {
        QAssignmentExpr assignment = null;
        final PsiElement parent = element.getParent();
        if (parent instanceof QAssignmentExpr a) {
            assignment = a;
        } else if (parent instanceof QParenthesesExpr p && p.getParent() instanceof QAssignmentExpr a) {
            assignment = a;
        }

        if (assignment == null) {
            return null;
        }

        final List<VarAssignment> varAssignments = assignment.getVarAssignments();
        for (VarAssignment varAssignment : varAssignments) {
            if (varAssignment.expression() == element) {
                return varAssignment;
            }
        }
        return null;
    }

    public static PsiElement getFirstNonWhitespaceAndCommentsChild(PsiElement el) {
        PsiElement c = el.getFirstChild();
        if (c == null) {
            return null;
        }
        return isWhitespace(c) ? PsiTreeUtil.skipWhitespacesAndCommentsForward(c) : c;
    }

    public static PsiElement findRootExpression(PsiElement element, PsiElement context) {
        if (element == null) {
            return null;
        }

        PsiElement cur = element;
        PsiElement parent = element.getParent();
        while (parent != null && parent != context && !(parent instanceof PsiFile)) {
            cur = parent;
            parent = parent.getParent();
        }
        return cur;
    }

    public static String createQualifiedName(String namespace, String identifier) {
        if (StringUtil.isEmpty(namespace)) {
            return identifier;
        }
        return namespace + "." + identifier;
    }

    public static QSymbol createSymbol(Project project, String name) {
        if (name.isEmpty() || name.charAt(0) != '`') {
            throw new IllegalArgumentException("Symbol must start with '`' char");
        }
        final QFile file = QFileType.createFactoryFile(project, name);
        return PsiTreeUtil.findChildOfType(file, QSymbol.class);
    }

    public static PsiElement createWhitespace(Project project, String text) {
        return project.getService(PsiParserFacade.class).createWhiteSpaceFromText(text);
    }

    public static PsiElement createColon(Project project) {
        return createCustomCode(project, ":");
    }

    public static PsiElement createSemicolon(Project project) {
        return createCustomCode(project, ";");
    }

    public static QVarReference createVarReference(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name), QVarReference.class);
    }

    /**
     * Does {@link PsiElement#getReferences()} and resolved all references taking into account {@link PsiPolyVariantReference#multiResolve(boolean)}
     */
    public static List<PsiElement> getResolvedReferences(PsiElement element) {
        final List<PsiElement> res = new ArrayList<>();
        final PsiReference[] references = element.getReferences();
        for (PsiReference reference : references) {
            if (reference instanceof PsiPolyVariantReference poly) {
                final ResolveResult[] resolveResults = poly.multiResolve(false);
                for (ResolveResult resolveResult : resolveResults) {
                    final PsiElement el = resolveResult.getElement();
                    if (resolveResult.isValidResult() && el != null) {
                        res.add(el);
                    }
                }
            } else {
                final PsiElement resolve = reference.resolve();
                if (resolve != null) {
                    res.add(resolve);
                }
            }
        }
        return res;
    }

    public static PsiElement clear(PsiElement element) {
        element.deleteChildRange(element.getFirstChild(), element.getLastChild());
        return element;
    }

    public static void moveCaret(PsiElement element, int offset) {
        final Editor editor = PsiEditorUtil.findEditor(element);
        if (editor != null) {
            moveCaret(editor, element, offset);
        }
    }

    public static void moveCaret(Editor editor, PsiElement element, int offset) {
        int pos = element.getTextOffset() + offset + (offset < 0 ? element.getTextLength() : 0);
        editor.getCaretModel().moveToOffset(pos);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    public static void selectInEditor(@NotNull Editor editor, @Nullable PsiElement element) {
        if (element == null) {
            return;
        }

        final TextRange range = element.getTextRange();
        final int offset = range.getStartOffset();
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    }

    public static PsiElement insert(Project project, PsiElement el, PsiElement parent, boolean blankLineBefore, boolean blankLineAfter) {
        PsiElement res = parent.add(el);
        parent.addAfter(QPsiUtil.createSemicolon(project), res);
        parent.addAfter(QPsiUtil.createWhitespace(project, "\n"), res);
        if (blankLineAfter) {
            parent.addAfter(QPsiUtil.createWhitespace(project, "\n"), res);
        }
        if (blankLineBefore) {
            parent.addBefore(QPsiUtil.createWhitespace(project, "\n"), res);
        }
        return res;
    }

    public static PsiElement insertAfter(Project project, PsiElement el, PsiElement anchor, boolean blankLine) {
        final PsiElement parent = anchor.getParent();
        PsiElement res = parent.addAfter(el, anchor);
        parent.addBefore(QPsiUtil.createWhitespace(project, "\n" + (blankLine ? '\n' : "")), res);
        parent.addAfter(QPsiUtil.createSemicolon(project), res);
        return res;
    }

    public static PsiElement insertBefore(Project project, PsiElement el, PsiElement anchor, boolean blankLine) {
        final PsiElement parent = anchor.getParent();
        PsiElement res = parent.addBefore(el, anchor);
        parent.addAfter(QPsiUtil.createSemicolon(project), res);
        parent.addBefore(QPsiUtil.createWhitespace(project, "\n" + (blankLine ? '\n' : "")), anchor);
        return res;
    }

    public static PsiElement createLambda(Project project, String name, boolean emptyBody, String... params) {
        final StringBuilder b = new StringBuilder(name);
        b.append('{');
        if (params != null && params.length > 0) {
            b.append('[');
            for (String param : params) {
                b.append(param).append("; ");
            }
            b.setLength(b.length() - 2);
            b.append(']');
        }
        b.append(emptyBody ? " " : '\n').append('}');
        return createCustomCode(project, b.toString());
    }

    public static PsiElement createCustomCode(Project project, String code) {
        return QFileType.createFactoryFile(project, code).getFirstChild();
    }

    public static QVarDeclaration createVarDeclaration(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name + ":`"), QVarDeclaration.class);
    }
}
