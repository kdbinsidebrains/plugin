package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class AssignmentBlock extends CodeBlock {
    public AssignmentBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final Alignment alignment = Alignment.createAlignment();
        return iterateChildren(myNode, child -> {
            final IElementType elementType = child.getElementType();
            if (elementType == QTypes.VAR_DECLARATION) {
                return new LeafBlock(child, formatter);
            }
            if (elementType == QTypes.ASSIGNMENT_TYPE) {
                return new LeafBlock(child, formatter, null, alignment, NORMAL_INDENT);
            }
            return createBlock(child, formatter, null, null, NORMAL_INDENT);
        });
    }
}
