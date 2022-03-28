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

public class LeafBlock extends AbstractQBlock {
    public LeafBlock(@NotNull ASTNode node, @NotNull QFormatter formatter) {
        super(node, formatter);
    }

    public LeafBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @NotNull Indent indent) {
        super(node, formatter, indent);
    }

    public LeafBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment) {
        super(node, formatter, wrap, alignment);
    }

    public LeafBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    protected List<Block> buildChildren() {
        return List.of();
    }
}