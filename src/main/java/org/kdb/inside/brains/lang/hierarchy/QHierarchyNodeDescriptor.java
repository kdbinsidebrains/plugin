package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QLambdaExpr;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Collection;
import java.util.List;

import static org.kdb.inside.brains.psi.QPsiUtil.getLambdaDescriptor;

public class QHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private final boolean recursion;
    private final List<SmartPsiElementPointer<PsiElement>> myUsages;

    protected QHierarchyNodeDescriptor(@NotNull Project project, @Nullable NodeDescriptor parent, @NotNull PsiElement element, boolean isBase) {
        this(project, parent, element, List.of(), isBase);
    }

    protected QHierarchyNodeDescriptor(@NotNull Project project, @Nullable NodeDescriptor parent, @NotNull PsiElement element, @NotNull Collection<? extends PsiElement> usages, boolean isBase) {
        super(project, parent, element, isBase);
        recursion = recursion(element);

        final var pointerManager = SmartPointerManager.getInstance(project);
        myUsages = ContainerUtil.map(usages, pointerManager::createSmartPsiElementPointer);
    }

    public boolean isRecursion() {
        return recursion;
    }

    @Override
    public boolean update() {
        boolean changes = super.update();
        final CompositeAppearance oldText = myHighlightedText;

        myHighlightedText = new CompositeAppearance();

        final NavigatablePsiElement element = (NavigatablePsiElement) getPsiElement();
        if (element == null) {
            return invalidElement();
        }

        final CompositeAppearance.DequeEnd ending = myHighlightedText.getEnding();

        installIcon(element, false);
        ending.addText(getNodeName(element));
        ending.addText(" ");

        int count = myUsages.size();
        if (count > 1) {
            String text = IdeBundle.message("node.call.hierarchy.N.usages", count);
            myHighlightedText.getEnding().addText(text, HierarchyNodeDescriptor.getUsageCountPrefixAttributes());
            ending.addText(" ");
        }

        ending.addText("(" + element.getContainingFile().getName() + ")", HierarchyNodeDescriptor.getPackageNameAttributes());

        if (isRecursion()) {
            ending.addText(" ");
            ending.addText("(recursion call)", SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
        }

        myName = myHighlightedText.getText();

        if (!Comparing.equal(myHighlightedText, oldText)) {
            changes = true;
        }
        return changes;
    }

    private String getNodeName(PsiElement element) {
        if (element instanceof QVarDeclaration dec) {
            final String name = dec.getQualifiedName();
            final PsiElement parent = dec.getParent();
            if (parent instanceof QAssignmentExpr a && a.getExpression() instanceof QLambdaExpr l) {
                return getLambdaDescriptor(name, l);
            }
        }
        if (element instanceof QVariable var) {
            return var.getQualifiedName();
        }
        if (element instanceof PsiFile f) {
            return f.getName();
        }
        return "Undefined Call: " + element.getText();
    }

    @Override
    public void navigate(boolean requestFocus) {
        Navigatable element = getNavigationTarget();
        if (element != null && element.canNavigate()) {
            element.navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        Navigatable element = getNavigationTarget();
        return element != null && element.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    private @Nullable Navigatable getNavigationTarget() {
        if (!myUsages.isEmpty() && myUsages.get(0).getElement() instanceof Navigatable nav) {
            return nav;
        }
        if (getPsiElement() instanceof Navigatable nav) {
            return nav;
        }
        return null;
    }

    private boolean recursion(PsiElement d) {
        NodeDescriptor<?> e = getParentDescriptor();
        while (e != null) {
            final PsiElement psiElement = ((HierarchyNodeDescriptor) e).getPsiElement();
            if (d.equals(psiElement)) {
                return true;
            }
            e = e.getParentDescriptor();
        }
        return false;
    }
}
