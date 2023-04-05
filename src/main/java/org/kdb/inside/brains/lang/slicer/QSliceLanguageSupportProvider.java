package org.kdb.inside.brains.lang.slicer;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.psi.PsiElement;
import com.intellij.slicer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;

public class QSliceLanguageSupportProvider implements SliceLanguageSupportProvider {
    @Override
    public @NotNull SliceUsage createRootUsage(@NotNull PsiElement element, @NotNull SliceAnalysisParams params) {
        return new QSliceUsage(element, params);
    }

    @Override
    public @Nullable PsiElement getExpressionAtCaret(@NotNull PsiElement element, boolean dataFlowToThis) {
        if (dataFlowToThis) {
            return null;
        }
        final PsiElement parent = element.getParent();
        if (parent instanceof QVarDeclaration && QPsiUtil.isGlobalDeclaration((QVarDeclaration) parent)) {
            return parent;
        }
        return null;
    }

    @Override
    public @NotNull PsiElement getElementForDescription(@NotNull PsiElement element) {
        return element;
    }

    @Override
    public @NotNull SliceUsageCellRendererBase getRenderer() {
        return new QSliceUsageCellRendererBase();
    }

    @Override
    public void startAnalyzeNullness(@NotNull AbstractTreeStructure structure, @NotNull Runnable finalRunnable) {
    }

    @Override
    public void startAnalyzeLeafValues(@NotNull AbstractTreeStructure structure, @NotNull Runnable finalRunnable) {
    }

    @Override
    public void registerExtraPanelActions(@NotNull DefaultActionGroup group, @NotNull SliceTreeBuilder builder) {
    }
}
