package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.navigation.ItemPresentation;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.Objects;
import java.util.Optional;

public abstract class InspectorElement implements CanonicalElement, StructureViewTreeElement, ItemPresentation {
    private final Icon icon;
    private final String name;
    private final String namespace;
    private final String canonicalName;

    private InspectorElement[] children;
    protected static final InspectorElement[] EMPTY_ARRAY = new InspectorElement[0];

    public InspectorElement(String name, String namespace, Icon icon) {
        this.name = name;
        this.namespace = namespace;
        this.icon = icon;
        this.canonicalName = namespace == null ? name : namespace + "." + name;
    }

    public static Optional<InspectorElement> unwrap(TreePath path) {
        if (path == null) {
            return Optional.empty();
        }
        final Object o = StructureViewComponent.unwrapWrapper(TreeUtil.getLastUserObject(path));
        return o instanceof InspectorElement ? Optional.of((InspectorElement) o) : Optional.empty();
    }

    public static <T extends InspectorElement> Optional<T> unwrap(TreePath path, Class<? extends T> type) {
        return unwrap(path).filter(e -> type.isAssignableFrom(e.getClass())).map(type::cast);
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public final @Nullable String getPresentableText() {
        return getName();
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public final Object getValue() {
        return this;
    }

    @Override
    public InspectorElement @NotNull [] getChildren() {
        if (children == null) {
            children = buildChildren();
        }
        return children;
    }

    protected InspectorElement[] buildChildren() {
        return EMPTY_ARRAY;
    }

    @Override
    public final @NotNull ItemPresentation getPresentation() {
        return this;
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InspectorElement that = (InspectorElement) o;
        return canonicalName.equals(that.canonicalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalName);
    }
}