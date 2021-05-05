package org.kdb.inside.brains.view.struct;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImportElement;
import org.kdb.inside.brains.psi.QVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QStructureViewElement implements StructureViewTreeElement, SortableTreeElement {
    private final NavigatablePsiElement element;

    public QStructureViewElement(NavigatablePsiElement element) {
        this.element = element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @NotNull
    @Override
    public String getAlphaSortKey() {
        if (element instanceof QVariable) {
            final QVariable var = (QVariable) element;
            return var.getQualifiedName();
        }
        final String name = element.getName();
        return name != null ? name : "";
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        final ItemPresentation presentation = element.getPresentation();
        return presentation != null ? presentation : new PresentationData();
    }

    @Override
    public void navigate(boolean requestFocus) {
        element.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return element.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element.canNavigateToSource();
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        final Collection<PsiElement> childrenOfType = PsiTreeUtil.findChildrenOfAnyType(element, QVariable.class, QImportElement.class);

        final List<TreeElement> treeElements = new ArrayList<>(childrenOfType.size());
        for (PsiElement el : childrenOfType) {
            if (el instanceof NavigatablePsiElement) {
                treeElements.add(new QStructureViewElement((NavigatablePsiElement) el));
            }
        }
/*
        for (PsiElement child : children) {
            if (child instanceof QContext) {

            } else {
                treeElements.add(new QStructureViewElement((NavigatablePsiElement) child));
            }
        }
*/

/*
        if (element instanceof QFile) {
            List<SimpleProperty> properties = PsiTreeUtil.getChildrenOfTypeAsList(myElement, SimpleProperty.class);
            List<TreeElement> treeElements = new ArrayList<>(properties.size());
            for (SimpleProperty property : properties) {
                treeElements.add(new SimpleStructureViewElement((SimplePropertyImpl) property));
            }
            return treeElements.toArray(new TreeElement[0]);
        }
*/
        return treeElements.toArray(new TreeElement[0]);
    }
}
