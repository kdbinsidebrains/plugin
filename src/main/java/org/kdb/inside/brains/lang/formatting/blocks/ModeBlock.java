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

public class ModeBlock extends AbstractQBlock {
    public ModeBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;
        final Wrap wrap = Wrap.createWrap(custom.MODE_WRAP_TYPE, false);
        final Alignment alignment = custom.MODE_ALIGN ? Alignment.createAlignment() : null;
        return iterateChildren(myNode, node -> {
            if (node.getElementType() == QTypes.MODE_PATTERN) {
                return new LeafBlock(node, formatter);
            }
            if (node.getElementType() == QTypes.SEMICOLON) {
                return createBlock(node, formatter, null, alignment, NORMAL_INDENT);
            }
            return createBlock(node, formatter, wrap, alignment, NORMAL_INDENT);
        });
    }
}
