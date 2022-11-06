package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class InspectorElement implements StructureViewTreeElement, ItemPresentation {
    private final Icon icon;
    private final String name;
    private final String namespace;
    private final String canonicalName;

    private static final InspectorElement[] EMPTY_ARRAY = new InspectorElement[0];
    private InspectorElement[] children;

    public InspectorElement(String name, String namespace, Icon icon) {
        this.name = name;
        this.namespace = namespace;
        this.icon = icon;
        this.canonicalName = namespace == null ? name : namespace + "." + name;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

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
    public final InspectorElement @NotNull [] getChildren() {
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
}