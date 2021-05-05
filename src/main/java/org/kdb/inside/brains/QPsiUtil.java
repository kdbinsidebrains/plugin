package org.kdb.inside.brains;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Optional;

public class QPsiUtil {
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
