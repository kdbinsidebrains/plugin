package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.formatting.QCodeStyleSettings;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class ParametersBlock extends AbstractQBlock {
    public ParametersBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @NotNull Indent indent) {
        super(node, formatter, null, null, indent);
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;

        final Wrap wrapCode = Wrap.createWrap(custom.LAMBDA_PARAMS_WRAP, true);
        final Alignment rootAlignment = Alignment.createAlignment();
        final Alignment bracketAlignment = custom.LAMBDA_PARAMS_ALIGN_BRACKETS ? Alignment.createChildAlignment(rootAlignment) : null;
        final Alignment expressionAlignment = custom.LAMBDA_PARAMS_ALIGN_NAMES ? Alignment.createChildAlignment(rootAlignment) : null;

        return iterateChildren(myNode, node -> {
            final IElementType type = node.getElementType();
            if (type == QTypes.BRACKET_OPEN) {
                return new LeafBlock(node, formatter, null, rootAlignment, NONE_INDENT);
            }
            if (type == QTypes.BRACKET_CLOSE) {
                return new LeafBlock(node, formatter, null, bracketAlignment, NONE_INDENT);
            }
            if (type == QTypes.SEMICOLON) {
                return new LeafBlock(node, formatter, null, expressionAlignment, NORMAL_INDENT);
            }
            return new LeafBlock(node, formatter, wrapCode, expressionAlignment, NORMAL_INDENT);
        });
    }
}
