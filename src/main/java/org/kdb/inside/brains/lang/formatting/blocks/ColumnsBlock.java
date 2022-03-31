package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QCodeStyleSettings;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class ColumnsBlock extends AbstractQBlock {
    public ColumnsBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;
        final Wrap wrap = Wrap.createWrap(custom.QUERY_WRAP_COLUMNS, false);
        final Alignment alignment = custom.QUERY_COLUMNS_ALIGN ? Alignment.createAlignment() : null;
        return iterateChildren((node, first) -> {
            final Wrap w = node.getElementType() == QTypes.QUERY_SPLITTER ? null : wrap;
            return createBlock(node, formatter, w, alignment, NORMAL_INDENT);
        });
    }
}
