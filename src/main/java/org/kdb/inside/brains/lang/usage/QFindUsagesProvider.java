package org.kdb.inside.brains.lang.usage;

import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.util.Optional;

/**
 * TODO: Symbol is not supported
 */
public final class QFindUsagesProvider implements FindUsagesProvider {
    @NotNull
    @Override
    public QWordsScanner getWordsScanner() {
        return new QWordsScanner();
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        if (psiElement instanceof QVariable || psiElement instanceof QSymbol) {
            return true;
//            final PsiElement context = psiElement.getContext();
//            return context instanceof QAssignment || context instanceof QParameters;
        }
        return false;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof QVariable) {
            return getFunctionDefinition((QVariable) element).isPresent() ? "function" : "variable";
        }
        return "";
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof QVariable) {
            return getDescriptiveName((QVariable) element);
        }
        return "";
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (!(element instanceof QVariable)) {
            return "";
        }
        final QVariable var = (QVariable) element;
        return useFullName ? var.getQualifiedName() : var.getName();
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
