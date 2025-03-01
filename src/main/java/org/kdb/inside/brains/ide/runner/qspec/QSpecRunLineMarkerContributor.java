package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;

import javax.swing.*;

public class QSpecRunLineMarkerContributor extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        final TestDescriptor descriptor = QSpecConfigurationProducer.getRunnableTestDescriptor(element);
        if (descriptor == null || descriptor.suite().hasNoCaption()) {
            return null;
        }

        if (descriptor.testCase() != null && descriptor.testCase().hasNoCaption()) {
            return null;
        }

//        if (TestDescriptor.findAllExpectations(descriptor.suite()).stream().filter(TestItem::isRunnable).anyMatch(TestItem::hasNoCaption)) {
//            return null;
//        }

        final String testUrl = descriptor.createUrl();
        final AnAction[] actions = ExecutorAction.getActions();
        final Icon icon = getTestStateIcon(testUrl, element.getProject(), false);
        return new Info(icon, new AnAction[]{actions[0], actions[actions.length - 1]}, RUN_TEST_TOOLTIP_PROVIDER);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}