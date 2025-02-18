package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class QSpecRunLineMarkerContributor extends RunLineMarkerContributor {
    private static @NotNull String createTestUrl(@NotNull PsiElement element, QSpecConfigurationProducer.TestPattern testPattern) {
        String testUrl = element.getContainingFile().getVirtualFile().getUrl() + "?";
        if (testPattern.specification() == null) {
            testUrl += "[]";
        } else {
            testUrl += "[" + testPattern.specification() + "]";
        }

        if (testPattern.expectation() != null) {
            testUrl += "/[" + testPattern.expectation() + "]";
        }
        return testUrl;
    }

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        final QSpecConfigurationProducer.TestPattern testPattern = QSpecConfigurationProducer.getTestPattern(element);
        if (testPattern == null) {
            return null;
        }

        final String testUrl = createTestUrl(element, testPattern);
        final AnAction[] actions = ExecutorAction.getActions();
        final Icon icon = getTestStateIcon(testUrl, element.getProject(), false);
        return new Info(icon, new AnAction[]{actions[0], actions[actions.length - 1]}, RUN_TEST_TOOLTIP_PROVIDER);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
