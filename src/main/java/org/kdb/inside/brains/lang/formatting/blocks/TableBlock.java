package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QCodeStyleSettings;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.ElementContext;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class TableBlock extends AbstractQBlock {
    public TableBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        if (ASTBlock.getElementType(child1) == QTypes.PAREN_OPEN && ASTBlock.getElementType(child2) == QTypes.TABLE_KEYS) {
            if (formatter.custom.TABLE_LBRACKET_NEW_LINE) {
                return Spacing.createDependentLFSpacing(0, 0, child2.getTextRange(), formatter.common.KEEP_LINE_BREAKS, formatter.common.KEEP_BLANK_LINES_IN_CODE);
            }
            return Spacing.createSpacing(0, 0, 0, formatter.common.KEEP_LINE_BREAKS, formatter.common.KEEP_BLANK_LINES_IN_CODE);
        }
        return super.getSpacing(child1, child2);
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;
        final Alignment parens = custom.TABLE_ALIGN_PARENS ? Alignment.createAlignment() : null;
        final Alignment alignment = custom.TABLE_ALIGN_COLUMNS ? Alignment.createAlignment() : null;
        return iterateChildren((node, first) -> {
            final IElementType type = node.getElementType();
            if (type == QTypes.PAREN_OPEN) {
                return new LeafBlock(node, formatter, null, parens, NONE_INDENT);
            }
            if (type == QTypes.PAREN_CLOSE) {
                final Indent indent = custom.TABLE_SPACE_BEFORE_GLOBAL_CLOSE_BRACKET && ElementContext.of(myNode) == null ? SPACE_INDENT : NONE_INDENT;
                return new LeafBlock(node, formatter, null, parens, indent);
            }
            if (type == QTypes.TABLE_KEYS) {
                final Wrap wrap = Wrap.createWrap(custom.TABLE_COLUMNS_WRAP, false);
                return new ColumnsBlock(node, formatter, wrap, alignment, NONE_INDENT);
            }
            if (type == QTypes.TABLE_VALUES) {
                final Wrap wrap = Wrap.createWrap(custom.TABLE_COLUMNS_WRAP, true);
                return new ColumnsBlock(node, formatter, wrap, alignment, NONE_INDENT);
            }
            return TableBlock.this.createBlock(node, formatter, null, null, NORMAL_INDENT);
        });
    }

    private static class ColumnsBlock extends AbstractQBlock {
        private final Wrap wrap;
        private final Alignment alignment;

        public ColumnsBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @NotNull Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
            super(node, formatter, null, null, indent);
            this.wrap = wrap;
            this.alignment = alignment;
        }

        @Override
        protected Indent getChildIndent() {
            return NORMAL_INDENT;
        }

        @Override
        protected List<Block> buildChildren() {
            final QCodeStyleSettings custom = formatter.custom;

            final Alignment brackets = custom.TABLE_ALIGN_BRACKETS ? Alignment.createAlignment() : null;

            return iterateChildren((node, first) -> {
                final IElementType type = node.getElementType();
                if (type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE) {
                    return new LeafBlock(node, formatter, null, brackets, NORMAL_INDENT);
                }
                if (type == QTypes.TABLE_COLUMN) {
                    return new CodeBlock(node, formatter, wrap, alignment, NORMAL_INDENT);
                }
                if (type == QTypes.SEMICOLON) {
                    return new LeafBlock(node, formatter, null, alignment, NORMAL_INDENT);
                }
                return createBlock(node, formatter, null, null, NORMAL_INDENT);
            });
        }
    }
}
