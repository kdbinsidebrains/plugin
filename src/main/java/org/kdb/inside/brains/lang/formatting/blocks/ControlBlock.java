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
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;

public class ControlBlock extends AbstractQBlock {
    public ControlBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings settings = formatter.custom;

        final ASTNode controlNode = getFirstNotEmptyChild(myNode);
        final ControlType controlType = ControlType.getControlType(controlNode);

        final Wrap wrapCode = Wrap.createWrap(controlType.wrapType(settings), false);
        final Alignment rootAlignment = Alignment.createAlignment();
        final Alignment bracketAlignment = controlType.alignBracket(settings) ? Alignment.createChildAlignment(rootAlignment) : null;
        final Alignment expressionAlignment = controlType.alignExpr(settings) ? Alignment.createChildAlignment(rootAlignment) : null;

        return iterateChildren((node, first) -> {
            final IElementType type = node.getElementType();
            if (controlNode == node) {
                return new LeafBlock(node, formatter);
            }
            if (type == QTypes.BRACKET_OPEN) {
                return new LeafBlock(node, formatter, null, rootAlignment, NORMAL_INDENT);
            }
            if (type == QTypes.BRACKET_CLOSE) {
                return new LeafBlock(node, formatter, null, bracketAlignment, SPACE_INDENT);
            }
            if (type == QTypes.SEMICOLON) {
                return new LeafBlock(node, formatter, null, expressionAlignment, NORMAL_INDENT);
            }
            return createBlock(node, formatter, wrapCode, expressionAlignment, NORMAL_INDENT);
        });
    }

    private enum ControlType {
        CONTROL() {
            @Override
            int wrapType(QCodeStyleSettings settings) {
                return settings.CONTROL_WRAP_TYPE;
            }

            @Override
            boolean alignExpr(QCodeStyleSettings settings) {
                return settings.CONTROL_ALIGN_EXPRS;
            }

            @Override
            boolean alignBracket(QCodeStyleSettings settings) {
                return settings.CONTROL_ALIGN_BRACKET;
            }
        },
        CONDITION() {
            @Override
            int wrapType(QCodeStyleSettings settings) {
                return settings.CONDITION_WRAP_TYPE;
            }

            @Override
            boolean alignExpr(QCodeStyleSettings settings) {
                return settings.CONDITION_ALIGN_EXPRS;
            }

            @Override
            boolean alignBracket(QCodeStyleSettings settings) {
                return settings.CONDITION_ALIGN_BRACKET;
            }
        };

        static ControlType getControlType(ASTNode node) {
            final IElementType elementType = node.getElementType();
            if (elementType == QTypes.CONDITION_KEYWORD) {
                return ControlType.CONDITION;
            } else if (elementType == QTypes.CONTROL_KEYWORD) {
                return ControlType.CONTROL;
            }
            throw new IllegalArgumentException("Unsupported control type: " + elementType);
        }

        abstract int wrapType(QCodeStyleSettings settings);

        abstract boolean alignExpr(QCodeStyleSettings settings);

        abstract boolean alignBracket(QCodeStyleSettings settings);
    }
}