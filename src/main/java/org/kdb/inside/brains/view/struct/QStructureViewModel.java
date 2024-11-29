package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import icons.KdbIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.EnumSet;

final class QStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider, StructureViewModel.ExpandInfoProvider {
    private static final Class[] CLASSES = {
            QFile.class, QImport.class, QCommand.class, QContext.class, QLambdaExpr.class, QAssignmentExpr.class, QTableColumn.class
    };

    public QStructureViewModel(@Nullable Editor editor, PsiFile psiFile) {
        super(psiFile, editor, new QStructureViewElement(psiFile));
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
                new SingleElementFilter("SHOW_TABLES", "Show Tables", StructureElementType.TABLE),
                new MultiElementFilter("SHOW_COLUMNS", "Show Columns", EnumSet.of(StructureElementType.TABLE_VALUE_COLUMN, StructureElementType.TABLE_KEY_COLUMN)),
                new SingleElementFilter("SHOW_IMPORTS", "Show Imports", StructureElementType.IMPORT),
                new SingleElementFilter("SHOW_COMMANDS", "Show Commands", StructureElementType.COMMAND),
                new SingleElementFilter("SHOW_LAMBDAS", "Show Lambdas", StructureElementType.LAMBDA),
                new SingleElementFilter("SHOW_VARIABLES", "Show Variables", StructureElementType.VARIABLE),
                new SingleElementFilter("SHOW_SYMBOLS", "Show Symbols", StructureElementType.SYMBOL),
                new TheVisibilityFilter(),
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

    @Override
    protected Class @NotNull [] getSuitableClasses() {
        return CLASSES;
    }

    private static class TheVisibilityFilter implements Filter {
        @Override
        public @NonNls @NotNull String getName() {
            return "SHOW_INTERNAL";
        }

        @Override
        public @NotNull ActionPresentation getPresentation() {
            return new ActionPresentationData("Show Internal Element", null, KdbIcons.Node.PrivateItem);
        }

        @Override
        public boolean isReverted() {
            return true;
        }

        @Override
        public boolean isVisible(TreeElement treeNode) {
            final PsiElement element = ((QStructureViewElement) treeNode).getElement();
            if (element instanceof QAssignmentExpr) {
                return QPsiUtil.isGlobalDeclaration((QAssignmentExpr) element);
            }
            return true;
        }
    }

    private static abstract class BaseElementFilter implements Filter {
        private final String name;
        private final ActionPresentationData presentation;

        public BaseElementFilter(String name, String text, Icon icon) {
            this.name = name;
            this.presentation = new ActionPresentationData(text, null, icon);
        }

        @Override
        public final @NonNls @NotNull String getName() {
            return name;
        }

        @Override
        public final @NotNull ActionPresentation getPresentation() {
            return presentation;
        }

        @Override
        public final boolean isReverted() {
            return true;
        }

        public final boolean isVisible(TreeElement treeNode) {
            return !containsType(((QStructureViewElement) treeNode).getType());
        }

        abstract boolean containsType(StructureElementType type);
    }

    private static class SingleElementFilter extends BaseElementFilter {
        private final StructureElementType type;

        public SingleElementFilter(String name, String text, StructureElementType type) {
            super(name, text, type.getIcon());
            this.type = type;
        }

        @Override
        boolean containsType(StructureElementType type) {
            return this.type == type;
        }
    }

    private static class MultiElementFilter extends BaseElementFilter {
        private final EnumSet<StructureElementType> types;

        private MultiElementFilter(String name, String text, EnumSet<StructureElementType> types) {
            super(name, text, types.stream().findFirst().map(StructureElementType::getIcon).orElse(null));
            this.types = types;
        }

        @Override
        boolean containsType(StructureElementType type) {
            return types.contains(type);
        }
    }
}
