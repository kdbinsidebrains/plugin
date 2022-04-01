package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;

import java.util.List;

public class IndentedBlock extends AbstractQBlock {
    public IndentedBlock(@NotNull ASTNode node, @NotNull QFormatter formatter) {
        super(node, formatter);
    }

    public IndentedBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final Alignment alignment = Alignment.createAlignment();
        return iterateChildren((node, first) -> IndentedBlock.this.createBlock(node, formatter, null, alignment, first ? NONE_INDENT : NORMAL_INDENT));
    }
}
