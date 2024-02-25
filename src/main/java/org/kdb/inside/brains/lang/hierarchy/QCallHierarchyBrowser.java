package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowserManager;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.PsiElement;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;

import javax.swing.*;
import java.util.Comparator;
import java.util.Map;

// PyCallHierarchyBrowser is used as an example
public class QCallHierarchyBrowser extends CallHierarchyBrowserBase {
    private static final Comparator<NodeDescriptor<?>> NODE_DESCRIPTOR_COMPARATOR = Comparator.comparingInt(NodeDescriptor::getIndex);

    public QCallHierarchyBrowser(@NotNull PsiElement qVar) {
        super(qVar.getProject(), qVar);
    }

    @Override
    protected @Nullable PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
        if (descriptor instanceof QHierarchyNodeDescriptor qd) {
            return qd.getPsiElement();
        }
        return null;
    }

    @Override
    protected void createTrees(@NotNull Map<? super @Nls String, ? super JTree> trees) {
        final ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction(IdeActions.GROUP_CALL_HIERARCHY_POPUP);

        trees.put(getCallerType(), createHierarchyTree(group));
        trees.put(getCalleeType(), createHierarchyTree(group));
    }

    private JTree createHierarchyTree(ActionGroup group) {
        final JTree tree = createTree(false);
        PopupHandler.installPopupMenu(tree, group, ActionPlaces.CALL_HIERARCHY_VIEW_POPUP);
        return tree;
    }

    @Override
    protected boolean isApplicableElement(@NotNull PsiElement element) {
        return element instanceof QVarDeclaration v && QPsiUtil.isGlobalDeclaration(v);
    }

    @Override
    protected @Nullable HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String type, @NotNull PsiElement psiElement) {
        if (getCallerType().equals(type)) {
            return new QCallerFunctionTreeStructure(myProject, psiElement, getCurrentScopeType());
        } else if (getCalleeType().equals(type)) {
            return new QCalleeFunctionTreeStructure(myProject, psiElement, getCurrentScopeType());
        } else {
            return null;
        }
    }

    @Override
    protected @Nullable Comparator<NodeDescriptor<?>> getComparator() {
        final HierarchyBrowserManager instance = HierarchyBrowserManager.getInstance(myProject);
        if (instance != null && instance.getState() != null && instance.getState().SORT_ALPHABETICALLY) {
            return AlphaComparator.INSTANCE;
        } else {
            return NODE_DESCRIPTOR_COMPARATOR;
        }
    }

    /*
    Defined in PyCallHierarchyBrowser but not sure it's correct
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
*/
}
