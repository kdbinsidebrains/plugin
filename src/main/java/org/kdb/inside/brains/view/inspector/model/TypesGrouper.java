package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TypesGrouper implements Grouper {
    public static final String ID = "GROUP_BY_TYPE";
    private final String name;
    private final ElementType elementType;

    public TypesGrouper(ElementType elementType) {
        this.name = ID + "_" + elementType.name();
        this.elementType = elementType;
    }

    @Override
    public @NonNls @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull ActionPresentation getPresentation() {
        return new ActionPresentationData("Group all " + elementType.getText(), null, elementType.getIcon());
    }

    @Override
    public @NotNull Collection<Group> group(@NotNull AbstractTreeNode<?> parent, @NotNull Collection<TreeElement> children) {
        if (parent.getValue() instanceof TypeGroup) {
            return Collections.emptyList();
        }

        final List<TreeElement> collect = children.stream().filter(elementType::isIt).collect(Collectors.toList());
        return List.of(new TypeGroup(elementType, collect));
    }

    public static class TypeGroup implements Group, ItemPresentation {
        private final ElementType type;
        private final Collection<TreeElement> children;

        public TypeGroup(ElementType type, Collection<TreeElement> children) {
            this.type = type;
            this.children = children;
        }

        @Override
        public String getPresentableText() {
            return type.getText();
        }

        @Override
        public @Nullable Icon getIcon(boolean unused) {
            return type.getIcon();
        }

        @Override
        public @NotNull ItemPresentation getPresentation() {
            return this;
        }

        @Override
        public @NotNull Collection<TreeElement> getChildren() {
            return children;
        }
    }
}
