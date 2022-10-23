package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.*;
import icons.KdbIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InspectorTreeModel implements StructureViewModel, StructureViewModel.ElementInfoProvider {
    private InstanceElement instanceElement = null;
    private final RootElement root = new RootElement();

    private final List<ModelListener> modelListeners = new CopyOnWriteArrayList<>();

    public static final String SHOW_TABLES = "SHOW_TABLES";
    public static final String SHOW_FUNCTIONS = "SHOW_FUNCTIONS";
    public static final String SHOW_VARIABLES = "SHOW_VARIABLES";
    public static final String SHOW_SYSTEM_NAMESPACES = "SHOW_SYSTEM_NAMESPACES";

    public InspectorTreeModel() {
    }

    public InstanceElement getInstanceElement() {
        return instanceElement;
    }

    @Override
    public @Nullable Object getCurrentEditorElement() {
        return null;
    }

    @Override
    public void addModelListener(@NotNull ModelListener modelListener) {
        modelListeners.add(modelListener);
    }

    @Override
    public void removeModelListener(@NotNull ModelListener modelListener) {
        modelListeners.remove(modelListener);
    }

    @Override
    public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {

    }

    @Override
    public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {

    }

    @Override
    public @NotNull StructureViewTreeElement getRoot() {
        return root;
    }

    @Override
    public Grouper @NotNull [] getGroupers() {
        return new Grouper[]{
                new TypesGrouper(ElementType.TABLES),
                new TypesGrouper(ElementType.FUNCTIONS),
                new TypesGrouper(ElementType.VARIABLES),
        };
    }

    @Override
    public Sorter @NotNull [] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public Filter @NotNull [] getFilters() {
        return new Filter[]{
                new TheElementFilter(SHOW_SYSTEM_NAMESPACES, "Show System Namespaces", KdbIcons.Node.SystemNamespaces, NamespaceElement.class) {
                    @Override
                    public boolean isVisible(TreeElement treeNode) {
                        if (treeNode instanceof NamespaceElement) {
                            final String n = ((NamespaceElement) treeNode).getCanonicalName();
                            if (n.length() == 2 && '.' == n.charAt(0)) {
                                final char ch = n.charAt(1);
                                return 'q' != ch && 'Q' != ch && 'z' != ch && 'h' != ch && 'j' != ch && 'o' != ch;
                            }
                        }
                        return true;
                    }
                },
                new TheElementFilter(SHOW_TABLES, "Show Tables", KdbIcons.Node.Table, TableElement.class),
                new TheElementFilter(SHOW_FUNCTIONS, "Show Functions", KdbIcons.Node.Function, FunctionElement.class),
                new TheElementFilter(SHOW_VARIABLES, "Show Variables", KdbIcons.Node.Variable, VariableElement.class),
        };
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean shouldEnterElement(Object element) {
        return false;
    }

    public void updateModel(InstanceElement instanceElement) {
        this.instanceElement = instanceElement;
        root.updateInstance(instanceElement);
        modelListeners.forEach(ModelListener::onModelChanged);
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element instanceof ExecutableElement;
    }

    private static class TheElementFilter implements Filter {
        private final String name;
        private final String text;
        private final Icon icon;
        private final Class<? extends InspectorElement> type;

        private TheElementFilter(String name, String text, Icon icon, Class<? extends InspectorElement> type) {
            this.name = name;
            this.text = text;
            this.icon = icon;
            this.type = type;
        }

        @Override
        public @NonNls
        @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull ActionPresentation getPresentation() {
            return new ActionPresentationData(text, null, icon);
        }

        @Override
        public boolean isVisible(TreeElement treeNode) {
            return treeNode instanceof NamespaceElement || !type.isAssignableFrom(treeNode.getClass());
        }

        @Override
        public boolean isReverted() {
            return true;
        }
    }
}