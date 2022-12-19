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
import java.util.Objects;
import java.util.stream.Collectors;

public class TypesGrouper implements Grouper {
    public static final String ID = "GROUP_BY_TYPE";
    private final String name;
    private final ElementType elementType;

    public TypesGrouper(ElementType elementType) {
        this.name = canonicalName(elementType);
        this.elementType = elementType;
    }

    @Override
    public @NonNls @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull ActionPresentation getPresentation() {
        return new ActionPresentationData("Group all " + elementType.getText(), null, elementType.getGroupIcon());
    }

    private static String canonicalName(ElementType type) {
        return ID + "_" + type.name();
    }

    @Override
    public @NotNull Collection<Group> group(@NotNull AbstractTreeNode<?> parent, @NotNull Collection<TreeElement> children) {
        final Object value = parent.getValue();
        if (value instanceof TypeGroup) {
            return Collections.emptyList();
        }

        final List<TreeElement> collect = children.stream().filter(elementType::isIt).collect(Collectors.toList());
        return List.of(new TypeGroup((CanonicalElement) value, elementType, collect));
    }

    public static class TypeGroup implements CanonicalElement, Group, ItemPresentation {
        private final ElementType type;
        private final String canonicalName;
        private final Collection<TreeElement> children;

        public TypeGroup(CanonicalElement parent, ElementType type, Collection<TreeElement> children) {
            this.type = type;
            this.children = children;

            final String prefix = parent instanceof InstanceElement ? "" : parent.getCanonicalName() + ".";
            this.canonicalName = prefix + canonicalName(type);
        }

        @Override
        public String getCanonicalName() {
            return canonicalName;
        }

        @Override
        public String getPresentableText() {
            return type.getText();
        }

        @Override
        public String getLocationString() {
            return null;
        }

        @Override
        public @Nullable Icon getIcon(boolean unused) {
            return type.getGroupIcon();
        }

        @Override
        public @NotNull ItemPresentation getPresentation() {
            return this;
        }

        @Override
        public @NotNull Collection<TreeElement> getChildren() {
            return children;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeGroup typeGroup = (TypeGroup) o;
            return Objects.equals(canonicalName, typeGroup.canonicalName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(canonicalName);
        }
    }
}
