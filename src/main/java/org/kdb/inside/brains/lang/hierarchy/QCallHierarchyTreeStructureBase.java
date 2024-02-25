package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QVarDeclaration;

import java.util.*;

public abstract class QCallHierarchyTreeStructureBase extends HierarchyTreeStructure {
    private final String myScopeType;

    protected QCallHierarchyTreeStructureBase(@NotNull Project project, PsiElement element, String currentScopeType) {
        super(project, new QHierarchyNodeDescriptor(project, null, element, true));
        this.myScopeType = currentScopeType;
    }

    @NotNull
    protected abstract Map<PsiElement, Collection<PsiElement>> getChildren(@NotNull QVarDeclaration element);

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        if (!(descriptor instanceof QHierarchyNodeDescriptor desc)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        if (desc.isRecursion()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        final PsiElement element = desc.getPsiElement();
        if (!(element instanceof QVarDeclaration declaration)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        final List<QHierarchyNodeDescriptor> descriptors = new ArrayList<>();

        final Map<PsiElement, Collection<PsiElement>> children = getChildren(declaration);
        final Map<PsiElement, QHierarchyNodeDescriptor> callerToDescriptorMap = new HashMap<>(children.size());
        children.forEach((c, u) -> {
            if (!isInScope(null, c, myScopeType)) {
                return;
            }

            QHierarchyNodeDescriptor callerDescriptor = callerToDescriptorMap.get(c);
            if (callerDescriptor == null) {
                callerDescriptor = new QHierarchyNodeDescriptor(descriptor.getProject(), descriptor, c, u, false);
                callerToDescriptorMap.put(c, callerDescriptor);
                descriptors.add(callerDescriptor);
            }
        });
        return ArrayUtil.toObjectArray(descriptors);
    }
}
