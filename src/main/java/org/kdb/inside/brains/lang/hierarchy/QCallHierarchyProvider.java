package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;

public class QCallHierarchyProvider implements HierarchyProvider {
    @Override
    public @Nullable PsiElement getTarget(@NotNull DataContext dataContext) {
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return null;
        }
        final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        if (element instanceof QVarDeclaration v && QPsiUtil.isGlobalDeclaration(v)) {
            return element;
        }
        return null;
    }

    @Override
    public @NotNull HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        return new QCallHierarchyBrowser(target);
    }

    @Override
    public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
        ((QCallHierarchyBrowser) hierarchyBrowser).changeView(CallHierarchyBrowserBase.getCallerType());
    }
}
