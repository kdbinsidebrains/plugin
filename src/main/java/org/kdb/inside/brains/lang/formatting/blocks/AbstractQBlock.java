package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;

import java.util.ArrayList;
import java.util.List;

import static org.kdb.inside.brains.lang.QNodeFactory.getFirstNotEmptyChild;
import static org.kdb.inside.brains.lang.QNodeFactory.getNextNotEmptySibling;
import static org.kdb.inside.brains.psi.QTypes.*;

public abstract class AbstractQBlock extends AbstractBlock {
    private final Indent indent;
    protected final QFormatter formatter;

    protected static final Indent SPACE_INDENT = Indent.getSpaceIndent(1);

    protected static final Indent NONE_INDENT = Indent.getNoneIndent();
    protected static final Indent ABSOLLUTE_NONE_INDENT = Indent.getAbsoluteNoneIndent();
    protected static final Indent NORMAL_INDENT = Indent.getNormalIndent();

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QFormatter formatter) {
        this(node, formatter, null, null, NONE_INDENT);
    }

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QFormatter formatter,
                          @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, wrap, alignment);
        this.formatter = formatter;
        this.indent = indent;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    @Override
    protected Indent getChildIndent() {
        return NONE_INDENT;
    }

    protected @Nullable Block createBlock(@Nullable ASTNode node, @NotNull QFormatter formatter) {
        return createBlock(node, formatter, NONE_INDENT);
    }

    protected @Nullable Block createBlock(@Nullable ASTNode node, @NotNull QFormatter formatter, @NotNull Indent indent) {
        return createBlock(node, formatter, null, null, indent);
    }

    protected @Nullable Block createBlock(@Nullable ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        if (node == null) {
            return null;
        }

        final IElementType type = node.getElementType();
        if (type == NEW_LINE) {
            return null;
        }

        if (type == BLOCK_COMMENT) { // block comment must never be formatted
            return new LeafBlock(node, formatter, null, null, ABSOLLUTE_NONE_INDENT);
        }

        if (node instanceof LeafPsiElement) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == MODE) {
            return new ModeBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == LAMBDA_EXPR) {
            return new LambdaBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == ASSIGNMENT_EXPR) {
            return new AssignmentBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QUERY_COLUMN || type == TYPED_PARAMETER || type == DIRECT_TYPED_VARIABLE || type == INVERTED_TYPED_VARIABLE) {
            return new AssignmentBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == ARGUMENTS) {
            return BracketsBlock.arguments(node, formatter, wrap, alignment, indent);
        }

        if (type == PARENTHESES_EXPR) {
            return BracketsBlock.parentheses(node, formatter, wrap, alignment, indent);
        }

        if (type == PATTERN_PARAMETER || type == PATTERN_DECLARATION) {
            return BracketsBlock.patterns(node, formatter, wrap, alignment, indent);
        }

        if (type == GROUPING_EXPR) {
            return BracketsBlock.grouping(node, formatter, wrap, alignment, indent);
        }

        if (type == CONDITION_EXPR || type == CONTROL_EXPR) {
            return new ControlBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QUERY_EXPR) {
            return new QueryBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == TABLE_EXPR || type == DICT_EXPR) {
            return new FlipBlock(node, formatter, wrap, alignment, indent);
        }

        if (InvokeBlock.isInvokeElement(type)) {
            return new InvokeBlock(node, formatter, wrap, alignment, indent);
        }

        // One value wrapper type which testCase be expanded
        if (isWrapperType(type)) {
            return createBlock(getFirstNotEmptyChild(node), formatter, wrap, alignment, indent);
        }

        if (isExpressionType(type)) {
            return new CodeBlock(node, formatter, wrap, alignment, indent);
        }

        if (isPrimitiveType(type)) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == TokenType.ERROR_ELEMENT) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        return new LeafBlock(node, formatter, wrap, alignment, indent);
    }

    private boolean isPrimitiveType(IElementType type) {
        return type == VAR_DECLARATION
                || type == VAR_REFERENCE
                || type == VAR_ASSIGNMENT_TYPE
                || type == TYPE_ASSIGNMENT_TYPE
                || type == SYMBOL
                || type == SYMBOLS
                || type == ITERATOR_TYPE
                ;
    }

    private boolean isWrapperType(IElementType type) {
        return type == LITERAL_EXPR
                || type == OPERATOR_TYPE
                || type == SYSTEM_FUNCTION
                || type == INTERNAL_FUNCTION
                ;
    }

    private boolean isExpressionType(IElementType type) {
        return type == EXPRESSIONS
                || type == VAR_ACCUMULATOR_TYPE
                || type == CUSTOM_FUNCTION
                || type == SIGNAL_EXPR
                || type == RETURN_EXPR
                || type == CONTEXT
                || type == CONTEXT_BODY
                || type == VAR_INDEXING
                || type == COMMAND
                || type == IMPORT_COMMAND
                || type == IMPORT_FUNCTION
                || type == K_SYNTAX_EXPR
                || type == PROJECTION_EXPR
                || type == TYPE_CAST_EXPR
                ;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return formatter.getSpacing(this, child1, child2);
    }

    protected final List<Block> iterateChildren(BlockCreator iterator) {
        final List<Block> result = new ArrayList<>();
        consumeChildren((node, first) -> {
            final Block block = iterator.newBlock(node, first);
            if (block != null) {
                result.add(block);
            }
        });
        return result;
    }

    protected final void consumeChildren(NodeConsumer consumer) {
        consumeChildren(myNode, consumer);
    }

    protected final void consumeChildren(ASTNode node, NodeConsumer consumer) {
        boolean first = true;
        ASTNode child = getFirstNotEmptyChild(node);
        while (child != null) {
            consumer.consumeNode(child, first);
            first = false;
            child = getNextNotEmptySibling(child);
        }
    }

    @FunctionalInterface
    protected interface BlockCreator {
        Block newBlock(ASTNode node, boolean first);
    }

    @FunctionalInterface
    protected interface NodeConsumer {
        void consumeNode(ASTNode node, boolean first);
    }
}
