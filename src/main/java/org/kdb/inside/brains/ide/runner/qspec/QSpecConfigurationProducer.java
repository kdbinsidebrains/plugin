package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.runner.KdbConfigurationProducerBase;
import org.kdb.inside.brains.psi.*;

import java.util.List;
import java.util.Objects;

public class QSpecConfigurationProducer extends KdbConfigurationProducerBase<QSpecRunConfiguration> {
    public static TestPattern getTestPattern(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        if (!(element instanceof QVarReference ref)) {
            return null;
        }

        final String qualifiedName = ref.getQualifiedName();
        if (qualifiedName.equals(QSpecTestLocator.QSPEC_SPECIFICATION_FUNCTION)) {
            final QInvokeFunction f = getInvokeFunction(ref);
            if (f == null || !(f.getParent() instanceof QFile)) {
                return null;
            }
            // root must be QFile
            final String name = getTestName(f);
            if (name != null) {
                return new TestPattern(name, null);
            }
        } else if (qualifiedName.equals(QSpecTestLocator.QSPEC_EXPECTATION_FUNCTION)) {
            final QInvokeFunction expF = getInvokeFunction(ref);
            if (expF == null) {
                return null;
            }

            final QInvokeFunction specF = getInvokeFunction(expF);
            if (specF == null || !(specF.getParent() instanceof QFile)) {
                return null;
            }

            final String expName = getTestName(expF);
            final String specName = getTestName(specF);
            return new TestPattern(specName, expName);
        }
        return null;
    }

    private static @Nullable QInvokeFunction getInvokeFunction(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, QInvokeFunction.class);
    }

    private static String getTestName(QInvokeFunction function) {
        if (function == null) {
            return null;
        }

        final List<QArguments> argumentsList = function.getArgumentsList();
        if (argumentsList.isEmpty()) {
            return null;
        }

        final List<QExpression> expressions = argumentsList.get(0).getExpressions();
        if (expressions.isEmpty()) {
            return null;
        }

        final QExpression qExpression = expressions.get(0);
        if (!(qExpression instanceof QLiteralExpr lit)) {
            return null;
        }
        final String text = lit.getText();
        return text.substring(1, text.length() - 1);
    }

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return QSpecConfigurationType.getInstance().getConfigurationFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull QSpecRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        if (!super.setupConfigurationFromContext(configuration, context, sourceElement)) {
            return false;
        }

        final Location<PsiElement> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final PsiElement element = location.getPsiElement();
        if (element instanceof QFile || element instanceof PsiDirectory) {
            return true;
        }

        final TestPattern testPattern = getTestPattern(element);
        if (testPattern == null) {
            return false;
        }

        configuration.setExpectationPattern(testPattern.expectation);
        configuration.setSpecificationPattern(testPattern.specification);
        configuration.setGeneratedName();
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull QSpecRunConfiguration configuration, @NotNull ConfigurationContext context) {
        if (!super.isConfigurationFromContext(configuration, context)) {
            return false;
        }
        final Location<PsiElement> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final PsiElement element = location.getPsiElement();
        if (element instanceof QFile || element instanceof PsiDirectory) {
            return true;
        }

        final TestPattern testPattern = getTestPattern(context.getPsiLocation());
        final String expectationPattern = configuration.getExpectationPattern();
        final String specificationPattern = configuration.getSpecificationPattern();
        if (expectationPattern == null && specificationPattern == null && testPattern == null) {
            return true;
        }
        final TestPattern p = new TestPattern(specificationPattern, expectationPattern);
        return Objects.equals(p, testPattern);
    }

    public record TestPattern(String specification, String expectation) {
    }
}
