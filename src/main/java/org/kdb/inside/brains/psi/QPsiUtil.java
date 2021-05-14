package org.kdb.inside.brains.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class QPsiUtil {


    public static AssignmentType getAssignmentType(QVariableElement variable) {
        final PsiElement parent = variable.getParent();
        if (parent instanceof QParameters) {
            return AssignmentType.PARAMETER;
        }

        if (parent instanceof QTableColumn) {
            return AssignmentType.TABLE_COLUMN;
        }
        if (parent instanceof QQueryColumn) {
            return AssignmentType.QUERY_COLUMN;
        }

        if (!(parent instanceof QVariableAssignment)) {
            return null;
        }

        final String qualifiedName = variable.getQualifiedName();
        if (QVariableElement.hasNamespace(qualifiedName)) {
            return AssignmentType.GLOBAL;
        }

        final QLambda lambda = variable.getContext(QLambda.class);
        if (lambda == null) {
            return AssignmentType.GLOBAL;
        }

        final PsiElement colon = PsiTreeUtil.findSiblingForward(variable, QTypes.COLON, true, null);
        if (colon != null && colon.getNextSibling().getNode().getElementType() == QTypes.COLON) {
            return AssignmentType.GLOBAL;
        }
        return AssignmentType.LOCAL;
    }

    @NotNull
    public static String getDescriptiveName(@NotNull QVariable element) {
        final String fqnOrName = element.getQualifiedName();
        return getFunctionDefinition(element).map((QLambda lambda) -> {
            final QParameters lambdaParams = lambda.getParameters();
            final String paramsText = Optional.ofNullable(lambdaParams)
                    .map(QParameters::getVariableList)
                    .map(params -> params.isEmpty() ? "" : lambdaParams.getText())
                    .orElse("");
            return fqnOrName + paramsText;
        }).orElse(fqnOrName);
    }

    public static Optional<QLambda> getFunctionDefinition(@NotNull QVariableElement element) {
        final QExpression expression = PsiTreeUtil.getNextSiblingOfType(element, QExpression.class);
        if (expression != null && expression.getFirstChild() instanceof QLambda) {
            return Optional.of((QLambda) expression.getFirstChild());
        }
        return Optional.empty();
    }

    public static boolean isImplicitVariable(QVariableElement variable) {
        final String qualifiedName = variable.getQualifiedName();
        if (QVariableElement.isImplicitVariable(qualifiedName)) {
            final QLambda enclosingLambda = variable.getContext(QLambda.class);
            return enclosingLambda != null && enclosingLambda.getParameters() == null;
        }
        return false;
    }
}
