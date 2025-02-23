package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class QSpecRunLineMarkerContributor extends RunLineMarkerContributor {
    private static @NotNull String createTestUrl(@NotNull PsiElement element, QSpecConfigurationProducer.TestPattern testPattern) {
        final String testUrl = FilenameUtils.normalize(element.getContainingFile().getVirtualFile().toNioPath().toAbsolutePath().toString(), true);
        final String expectation = testPattern.expectation();
        final String specification = testPattern.specification();
        final String specId = specification == null ? "?[]" : "?[" + specification + "]";
        if (expectation != null) {
            return "qspec:test://" + testUrl + specId + "/[" + expectation + "]";
        } else if (specification != null) {
            return "qspec:suite://" + testUrl + specId;
        }
        return "qspec:script://" + testUrl;
    }

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        final QSpecConfigurationProducer.TestPattern testPattern = QSpecConfigurationProducer.getTestPattern(element);
        if (testPattern == null) {
            return null;
        }

        if (ModuleUtilCore.findModuleForPsiElement(element) == null) {
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
