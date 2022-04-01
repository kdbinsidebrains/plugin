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
        final Alignment alignment = custom.QUERY_PARTS_ALIGN ? Alignment.createAlignment(false, Alignment.Anchor.RIGHT) : null;

        return iterateChildren((node, first) -> {
            final IElementType type = node.getElementType();

            if (type == QTypes.QUERY_TYPE) {
                return new LeafBlock(node, formatter, wrap, alignment, NONE_INDENT);
            }

            if (type == QTypes.QUERY_BY || type == QTypes.QUERY_FROM || type == QTypes.QUERY_WHERE) {
                return new LeafBlock(node, formatter, wrap, alignment, NORMAL_INDENT);
            }

            if (type == QTypes.QUERY_COLUMNS) {
                return new ColumnsBlock(node, formatter, null, null, NORMAL_INDENT);
            }
            return createBlock(node, formatter, null, null, NORMAL_INDENT);
        });
    }
}
