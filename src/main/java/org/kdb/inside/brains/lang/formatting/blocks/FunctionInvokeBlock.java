package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class FunctionInvokeBlock extends IndentedBlock {
    public FunctionInvokeBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        // Special case - Accumulator must have 1 space before
        if (ASTBlock.getElementType(child1) != QTypes.ITERATOR_TYPE && ASTBlock.getElementType(child2) == QTypes.ITERATOR_TYPE) {
            final List<Block> subBlocks = child2.getSubBlocks();
            if (subBlocks.size() > 0 && ASTBlock.getElementType(subBlocks.get(0)) == QTypes.ACCUMULATOR) {
                final CommonCodeStyleSettings common = formatter.common;
                return Spacing.createSpacing(1, 1, 0, common.KEEP_LINE_BREAKS, common.KEEP_BLANK_LINES_IN_CODE);
            }
        }
        return super.getSpacing(child1, child2);
    }
}
