package org.kdb.inside.brains.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.kdb.inside.brains.QFileType;

public final class QPsiUtil {
    private QPsiUtil() {
    }

    public static String getTypeCast(QTypeCastExpr cast) {
        final String text = cast.getTypeCast().getText();
        final String name = text.charAt(0) == '`' ? text.substring(1, text.length() - 1) : text.substring(1, text.length() - 2);
        if (name.isEmpty()) {
            return "symbol";
        }
        return name;
    }

    public static boolean hasNamespace(String identifier) {
        return !identifier.isEmpty() && identifier.charAt(0) == '.';
    }

    public static boolean isImplicitVariable(QVariable variable) {
        final String qualifiedName = variable.getQualifiedName();
        if (QVariable.IMPLICIT_VARS.contains(qualifiedName)) {
            final QLambdaExpr enclosingLambda = variable.getContext(QLambdaExpr.class);
            return enclosingLambda != null && enclosingLambda.getParameters() == null;
        }
        return false;
    }

    public static boolean isGlobalDeclaration(QVarDeclaration declaration) {
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
                if (el1 != null) {
                    final PsiElement el2 = PsiTreeUtil.skipWhitespacesAndCommentsForward(el1);
                    return QPsiUtil.isColon(el1) && QPsiUtil.isColon(el2);
                }
                return false;
        }
        return false;
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

    public static boolean isSemicolon(PsiElement el) {
        return isLeafText(el, ";");
    }

    public static boolean isLineBreak(PsiElement el) {
        return el != null && el.getNode().getElementType() == QTypes.LINE_BREAK;
    }

    public static boolean isLeafText(PsiElement el, String text) {
        return el instanceof LeafPsiElement && text.equals(el.getText());
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
        if (name.length() == 0 || name.charAt(0) != '`') {
            throw new IllegalArgumentException("Symbol must start with '`' char");
        }
        final QFile file = QFileType.createFactoryFile(project, name);
        return PsiTreeUtil.findChildOfType(file, QSymbol.class);
    }

    public static QVarReference createVarReference(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name), QVarReference.class);
    }

    public static QVarDeclaration createVarDeclaration(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name + ":`"), QVarDeclaration.class);
    }
}
