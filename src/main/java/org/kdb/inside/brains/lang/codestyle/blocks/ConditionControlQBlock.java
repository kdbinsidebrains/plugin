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
        return iterateNotEmptyChildren(myNode, new Context()::createBlock);
    }

    private class Context {
        private Indent indent;

        private Indent defaultIndent = null;
        private boolean firstExpression = true;

        private final Alignment bracket = Alignment.createAlignment();
        private final Alignment expression = getAlignmentOption() ? Alignment.createChildAlignment(bracket) : null;

        private final Wrap expressionsWrap = Wrap.createWrap(getWrapOption(), false);

        Block createBlock(ASTNode node) {
            final IElementType type = node.getElementType();

            // If no default indent - it's first element so we use it's length + 1 for bracket
            if (defaultIndent == null) {
                defaultIndent = Indent.getNormalIndent(false);
            } else {
                // and re-use it for all other elements
                indent = defaultIndent;
            }

            final Wrap wrap = getWrap(type);
            final Alignment alignment = getAlignment(type);

            return ConditionControlQBlock.this.createBlock(node, wrap, indent, alignment);
        }

        private Alignment getAlignment(IElementType type) {
            if (indent == null) {
                return null;
            }

            if (type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE) {
                return bracket;
            }
            return expression;
        }

        @Nullable
        private Wrap getWrap(IElementType type) {
            if (type == QTypes.CONTROL_EXPR || type == QTypes.CONDITION_EXPR || type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE || type == QTypes.SEMICOLON) {
                return null;
            }

            if (firstExpression) {
                firstExpression = false;
                return null;
            }
            return expressionsWrap;
        }

        private int getWrapOption() {
            return elementType == QTypes.CONTROL_EXPR ? qSettings.CONTROL_WRAP_TYPE : qSettings.CONDITION_WRAP_TYPE;
        }

        private boolean getAlignmentOption() {
            return elementType == QTypes.CONTROL_EXPR ? qSettings.CONTROL_WRAP_ALIGN : qSettings.CONDITION_WRAP_ALIGN;
        }
    }
}
