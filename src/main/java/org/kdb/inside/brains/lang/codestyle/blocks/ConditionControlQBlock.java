package org.kdb.inside.brains.lang.codestyle.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.codestyle.QCodeStyleSettings;
import org.kdb.inside.brains.lang.codestyle.QSpacingStrategy;
import org.kdb.inside.brains.psi.QTypes;

import java.util.ArrayList;
import java.util.List;

public class ConditionControlQBlock extends AbstractQBlock {
    private final IElementType elementType;

    public ConditionControlQBlock(@NotNull ASTNode node, @NotNull QSpacingStrategy spacingStrategy, @NotNull QCodeStyleSettings qSettings, @NotNull CommonCodeStyleSettings settings, @Nullable Wrap wrap, @Nullable Indent indent, @Nullable Alignment alignment, IElementType elementType) {
        super(node, spacingStrategy, qSettings, settings, wrap, indent, alignment);
        if (elementType != QTypes.CONTROL_EXPR && elementType != QTypes.CONDITION_EXPR) {
            throw new IllegalArgumentException("Unsupported element type: " + elementType);
        }
        this.elementType = elementType;
    }

    @Override
    protected List<Block> buildChildren() {
        return processNode(myNode, new ArrayList<>(), new Context());
    }

    private List<Block> processNode(ASTNode parentNode, List<Block> blocks, Context context) {
        iterateNotEmptyChildren(parentNode, node -> {
            final IElementType elementType = node.getElementType();
            if (elementType == QTypes.ARGUMENTS) {
//            if (elementType == QTypes.ARGUMENTS || elementType == QTypes.STATEMENT) {
                processNode(node, blocks, context);
            } else {
                context.swallowNextNode(node);

                blocks.add(createBlock(node, context.wrap, context.indent, context.alignment));
            }
        });
        return blocks;
    }

    private class Context {
        private Wrap wrap;
        private Indent indent;
        private Alignment alignment;

        private Indent defaultIndent = null;

        private final Alignment bracket = Alignment.createAlignment();
        private final Alignment expression = getAlignmentOption() ? Alignment.createChildAlignment(bracket) : null;

        private final Wrap expressionsWrap = Wrap.createWrap(getWrapOption(), false);

        void swallowNextNode(ASTNode node) {
            // If no default indent - it's first element so we use it's length + 1 for bracket
            if (defaultIndent == null) {
                defaultIndent = Indent.getNormalIndent(false);
            } else {
                // and re-use it for all other elements
                indent = defaultIndent;
            }

            final IElementType type = node.getElementType();
            if (type == QTypes.EXPRESSION) {
                wrap = expressionsWrap;
            } else {
                wrap = null;
            }

            if (indent == null) {
                alignment = null;
            } else if (type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE) {
                alignment = bracket;
            } else {
                alignment = expression;
            }
        }

        private int getWrapOption() {
            return elementType == QTypes.CONTROL_EXPR ? qSettings.CONTROL_WRAP_TYPE : qSettings.CONDITION_WRAP_TYPE;
        }

        private boolean getAlignmentOption() {
            return elementType == QTypes.CONTROL_EXPR ? qSettings.CONTROL_WRAP_ALIGN : qSettings.CONDITION_WRAP_ALIGN;
        }
    }
}
