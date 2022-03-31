package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QCodeStyleSettings;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class QueryBlock extends AbstractQBlock {
    public QueryBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;

        final Wrap wrap = Wrap.createWrap(custom.QUERY_WRAP_PARTS, false);
        final Alignment alignment = custom.QUERY_PARTS_ALIGN ? Alignment.createAlignment(true, Alignment.Anchor.RIGHT) : null;

        return iterateChildren((node, first) -> {
            final IElementType type = node.getElementType();

            final Indent indent = first ? NONE_INDENT : NORMAL_INDENT;
            final Wrap w = wrappingType(type) ? wrap : null;
            return createBlock(node, formatter, w, alignment, indent);
        });
    }

    private boolean wrappingType(IElementType type) {
        return type == QTypes.QUERY_TYPE || type == QTypes.QUERY_BY || type == QTypes.QUERY_FROM;
    }
}
