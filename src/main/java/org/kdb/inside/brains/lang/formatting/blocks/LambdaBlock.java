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
import org.kdb.inside.brains.psi.ElementContext;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class LambdaBlock extends AbstractQBlock {
    public LambdaBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;

        final Alignment braceAlignment = custom.LAMBDA_ALIGN_BRACE ? Alignment.createAlignment() : null;

        return iterateChildren(child -> {
            final IElementType elementType = child.getElementType();
            if (elementType == QTypes.PARAMETERS) {
                return BracketsBlock.parameters(child, formatter, null, null, NORMAL_INDENT);
            }
            if (elementType == QTypes.EXPRESSIONS) {
                return createBlock(child, formatter, null, null, NORMAL_INDENT);
            }
            if (elementType == QTypes.BRACE_OPEN) {
                return new LeafBlock(child, formatter, null, braceAlignment, NONE_INDENT);
            }
            if (elementType == QTypes.BRACE_CLOSE) {
                final Indent indent = custom.LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE && ElementContext.of(myNode) == null ? SPACE_INDENT : NONE_INDENT;
                return new LeafBlock(child, formatter, null, braceAlignment, indent);
            }
            return new LeafBlock(child, formatter);
        });
    }
}
