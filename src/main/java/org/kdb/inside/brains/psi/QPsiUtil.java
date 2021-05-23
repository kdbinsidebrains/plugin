package org.kdb.inside.brains.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

import java.util.Optional;

public final class QPsiUtil {
    private QPsiUtil() {
    }

    public static String getTypeCast(QTypeCast cast) {
        final String text = cast.getFirstChild().getText();
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
            final QLambda enclosingLambda = variable.getContext(QLambda.class);
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
                final String qualifiedName = declaration.getQualifiedName();
                if (hasNamespace(qualifiedName)) {
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

    public static boolean isColon(PsiElement el) {
        return el instanceof LeafPsiElement && ":".equals(el.getText());
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


    @Deprecated
    public static ElementScope getAssignmentType(QVariable variable) {
        final PsiElement parent = variable.getParent();
        if (parent instanceof QParameters) {
            return ElementScope.PARAMETERS;
        }

        if (parent instanceof QTableColumn) {
            return ElementScope.TABLE;
        }
        if (parent instanceof QQueryColumn) {
            return ElementScope.QUERY;
        }

        if (!(parent instanceof QVariableAssignment)) {
            return null;
        }

        final String qualifiedName = variable.getQualifiedName();
        if (hasNamespace(qualifiedName)) {
            return ElementScope.FILE;
        }

        final QLambda lambda = variable.getContext(QLambda.class);
        if (lambda == null) {
            return ElementScope.FILE;
        }

        final PsiElement colon = PsiTreeUtil.findSiblingForward(variable, QTypes.COLON, true, null);
        if (colon != null && colon.getNextSibling().getNode().getElementType() == QTypes.COLON) {
            return ElementScope.FILE;
        }
        return ElementScope.LAMBDA;
    }

    @NotNull
    @Deprecated
    public static String getDescriptiveName(@NotNull QVariable element) {
        final String fqnOrName = element.getQualifiedName();
        return getFunctionDefinition(element).map((QLambda lambda) -> {
            final QParameters lambdaParams = lambda.getParameters();
            final String paramsText = Optional.ofNullable(lambdaParams)
                    .map(QParameters::getVariables)
                    .map(params -> params.isEmpty() ? "" : lambdaParams.getText())
                    .orElse("");
            return fqnOrName + paramsText;
        }).orElse(fqnOrName);
    }

    @Deprecated
    public static Optional<QLambda> getFunctionDefinition(@NotNull QVariable element) {
        final QExpression expression = PsiTreeUtil.getNextSiblingOfType(element, QExpression.class);
        if (expression != null && expression.getFirstChild() instanceof QLambda) {
            return Optional.of((QLambda) expression.getFirstChild());
        }
        return Optional.empty();
    }
}
