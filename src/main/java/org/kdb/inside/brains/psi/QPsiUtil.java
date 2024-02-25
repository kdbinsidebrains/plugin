package org.kdb.inside.brains.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;

import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class QPsiUtil {
    private QPsiUtil() {
    }

    public static boolean isKeyColumn(@Nullable QTableColumn column) {
        return column != null && column.getParent() instanceof QTableKeys;
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

    public static boolean isImplicitVariable(@NotNull QVariable variable) {
        final String qualifiedName = variable.getQualifiedName();
        if (QVariable.IMPLICIT_VARS.contains(qualifiedName)) {
            final QLambdaExpr enclosingLambda = variable.getContext(QLambdaExpr.class);
            return enclosingLambda != null && enclosingLambda.getParameters() == null;
        }
        return false;
    }

    public static boolean isGlobalDeclaration(@NotNull QAssignmentExpr assignment) {
        final QVarDeclaration varDeclaration = assignment.getVarDeclaration();
        return varDeclaration != null && isGlobalDeclaration(varDeclaration);
    }

    public static boolean isGlobalDeclaration(@NotNull QVarDeclaration declaration) {
        final ElementContext of = ElementContext.of(declaration);
        switch (of.getScope()) {
            case TABLE:
            case QUERY:
            case PARAMETERS:
                return false;
            case FILE:
                return true;
            case LAMBDA:
                if (hasNamespace(declaration.getName())) {
                    return true;
                }
                final PsiElement el1 = PsiTreeUtil.skipWhitespacesAndCommentsForward(declaration);
                return el1 instanceof QAssignmentType && "::".equals(el1.getText());
        }
        return false;
    }

    public static String getLambdaDescriptor(String name, QLambdaExpr lambda) {
        final QParameters parameters = lambda.getParameters();
        if (parameters == null) {
            return name + "[]";
        } else {
            final String collect = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
            return name + "[" + collect + "]";
        }
    }

    /**
     * Checks is the specified element colon or not.
     *
     * @param el the element to be checked
     * @return <code>true</code> if the elemtn is colon; <code>false</code> - otherwise.
     */
    public static boolean isColon(PsiElement el) {
        return isLeafText(el, ":");
    }

    public static boolean isWhitespace(PsiElement el) {
        return el instanceof PsiWhiteSpace || el instanceof PsiComment;
    }

    public static boolean isSemicolon(PsiElement el) {
        return isLeafText(el, ";");
    }

    public static boolean isLeafText(PsiElement el, String text) {
        return el instanceof LeafPsiElement && text.equals(el.getText());
    }

    public static boolean isLeafText(PsiElement el, Predicate<String> predicate) {
        return el instanceof LeafPsiElement && predicate.test(el.getText());
    }

    public static PsiElement getFirstNonWhitespaceAndCommentsChild(PsiElement el) {
        PsiElement c = el.getFirstChild();
        if (c == null) {
            return null;
        }
        return isWhitespace(c) ? PsiTreeUtil.skipWhitespacesAndCommentsForward(c) : c;
    }

    public static PsiElement findRootExpression(PsiElement element) {
        if (element == null) {
            return null;
        }
        return findRootExpression(element, null);
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

    public static ElementContext getElementContext(QPsiElement element) {
        return ElementContext.of(element);
    }

    public static String createQualifiedName(String namespace, String identifier) {
        if (namespace == null || namespace.isEmpty()) {
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

    public static PsiElement createSemicolon(Project project) {
        return QFileType.createFactoryFile(project, ";").getFirstChild();
    }

    public static QVarReference createVarReference(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name), QVarReference.class);
    }

    public static PsiElement createLambdaDeclaration(Project project, boolean global, String name, String... params) {
        final StringBuilder b = new StringBuilder(name);
        b.append(":{");
        if (params != null && params.length != 0) {
            b.append('[').append(String.join("; ", params)).append(']');
        }
        b.append(global ? '\n' : "  ");
        b.append('}');
        return QFileType.createFactoryFile(project, b.toString()).getFirstChild();
    }

    public static String getImportContent(QImport qImport) {
        if (qImport instanceof QImportFunction f && f.getExpression() != null) {
            String text = "\"" + f.getExpression().getText().trim().substring(3); // remove 'l ';
            if (text.startsWith("\"\"")) {
                text = text.substring(2);
            }
            if (text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
                text = text.substring(1, text.length() - 1);
            }
            if (text.charAt(0) == ',') {
                text = text.substring(1);
            }
            return text;
        }
        return qImport.getFilePath();
    }

    public static QVarDeclaration createVarDeclaration(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name + ":`"), QVarDeclaration.class);
    }
}
