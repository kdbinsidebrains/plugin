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
                new TheElementFilter("SHOW_TABLES", "Show Tables", StructureElementType.TABLE),
                new TheElementFilter("SHOW_COLUMNS", "Show Columns", EnumSet.of(StructureElementType.TABLE_VALUE_COLUMN, StructureElementType.TABLE_KEY_COLUMN)),
                new TheElementFilter("SHOW_IMPORTS", "Show Imports", StructureElementType.IMPORT),
                new TheElementFilter("SHOW_COMMANDS", "Show Commands", StructureElementType.COMMAND),
                new TheElementFilter("SHOW_LAMBDAS", "Show Lambdas", StructureElementType.LAMBDA),
                new TheElementFilter("SHOW_VARIABLES", "Show Variables", StructureElementType.VARIABLE),
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

    private static class TheElementFilter implements Filter {
        private final String name;
        private final String text;
        private final Icon icon;
        private final EnumSet<StructureElementType> types;

        private TheElementFilter(String name, String text, StructureElementType type) {
            this(name, text, EnumSet.of(type));
        }

        private TheElementFilter(String name, String text, EnumSet<StructureElementType> types) {
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
