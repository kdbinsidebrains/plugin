package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.runner.KdbConfigurationProducerBase;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QVarReference;

import java.util.Objects;

public class QSpecConfigurationProducer extends KdbConfigurationProducerBase<QSpecRunConfiguration> {
    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return QSpecConfigurationType.getInstance().getConfigurationFactory();
    }

    static TestDescriptor getRunnableTestDescriptor(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }

        if (!(element.getParent() instanceof QVarReference ref)) {
            return null;
        }

        if (!TestDescriptor.isRunnableItem(ref)) {
            return null;
        }

        final TestDescriptor descriptor = TestDescriptor.of(ref);
        if (descriptor == null) {
            return null;
        }

        if (ModuleUtilCore.findModuleForPsiElement(element) == null) {
            return null;
        }

        return descriptor;
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
        if (element instanceof QFile f) {
            return TestDescriptor.hasTestCases(f);
        }

        if (element instanceof PsiDirectory dir) {
            return containsTestFiles(dir);
        }

        final TestDescriptor descriptor = getRunnableTestDescriptor(element);
        if (descriptor == null) {
            return false;
        }

        configuration.setSuitePattern(descriptor.suite().getCaption());

        if (descriptor.testCase() != null) {
            configuration.setTestPattern(descriptor.testCase().getCaption());
        }
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

        final String descPattern = configuration.getSuitePattern();
        final String shouldPattern = configuration.getTestPattern();

        final PsiElement element = location.getPsiElement();
        if ((element instanceof QFile || element instanceof PsiDirectory) && StringUtil.isEmpty(descPattern) && StringUtil.isEmpty(shouldPattern)) {
            return true;
        }

        final TestDescriptor descriptor = getRunnableTestDescriptor(context.getPsiLocation());
        if (descriptor == null) {
            return StringUtil.isEmpty(descPattern) && StringUtil.isEmpty(shouldPattern);
        }
        return compareItemName(descriptor.suite(), descPattern) && compareItemName(descriptor.testCase(), shouldPattern);
    }

    private boolean compareItemName(TestItem item, String value) {
        return (item == null && StringUtil.isEmpty(value)) || (item != null && Objects.equals(item.getCaption(), value));
    }

    private boolean containsTestFiles(PsiDirectory directory) {
        // Check files in the current directory
        for (PsiFile file : directory.getFiles()) {
            if (file.getFileType() == QFileType.INSTANCE) {
                return true;
            }
        }

        // Recursively check subdirectories
        for (PsiDirectory subDir : directory.getSubdirectories()) {
            if (containsTestFiles(subDir)) {
                return true;
            }
        }
        return false;
    }
}
