package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;

final class QStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider, StructureViewModel.ExpandInfoProvider {
    public QStructureViewModel(PsiFile psiFile) {
        super(psiFile, new QStructureViewElement(psiFile));
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return ((QStructureViewElement) element).getType().isAlwaysLeaf();
    }

    @Override
    public Sorter @NotNull [] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public Filter @NotNull [] getFilters() {
        return new Filter[]{
                new TheFilter("SHOW_TABLES", "Show Tables", StructureElementType.TABLE),
                new TheFilter("SHOW_COLUMNS", "Show Columns", EnumSet.of(StructureElementType.TABLE_VALUE_COLUMN, StructureElementType.TABLE_KEY_COLUMN)),
                new TheFilter("SHOW_IMPORTS", "Show Imports", StructureElementType.LOAD),
                new TheFilter("SHOW_COMMANDS", "Show Commands", StructureElementType.COMMAND),
                new TheFilter("SHOW_LAMBDAS", "Show Lambdas", StructureElementType.LAMBDA),
                new TheFilter("SHOW_VARIABLES", "Show Variables", StructureElementType.VARIABLE),
        };
    }

    @Override
    public boolean isSmartExpand() {
        return true;
    }

    @Override
    public boolean isAutoExpand(@NotNull StructureViewTreeElement element) {
        return ((QStructureViewElement) element).getType().isAutoExpand();
    }

    private static class TheFilter implements Filter {
        private final String name;
        private final String text;
        private final Icon icon;
        private final EnumSet<StructureElementType> types;

        private TheFilter(String name, String text, StructureElementType type) {
            this(name, text, EnumSet.of(type));
        }

        private TheFilter(String name, String text, EnumSet<StructureElementType> types) {
            this.name = name;
            this.text = text;
            this.icon = types.stream().findFirst().map(StructureElementType::getIcon).orElse(null);
            this.types = types;
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
            return !types.contains(((QStructureViewElement) treeNode).getType());
        }

        @Override
        public boolean isReverted() {
            return true;
        }
    }
}
