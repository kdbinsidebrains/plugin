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
import org.kdb.inside.brains.psi.QTypes;

import java.util.ArrayList;
import java.util.List;

import static org.kdb.inside.brains.lang.QNodeFactory.getFirstNotEmptyChild;
import static org.kdb.inside.brains.lang.QNodeFactory.getNextNotEmptySibling;

public abstract class AbstractQBlock extends AbstractBlock {
    private final Indent indent;
    protected static final Indent SPACE_INDENT = Indent.getSpaceIndent(1);

    protected static final Indent NONE_INDENT = Indent.getNoneIndent();
    protected static final Indent ABSOLLUTE_NONE_INDENT = Indent.getAbsoluteNoneIndent();
    protected static final Indent NORMAL_INDENT = Indent.getNormalIndent();
    protected final QFormatter formatter;

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
        if (type == QTypes.NEW_LINE) {
            return null;
        }

        if (type == QTypes.BLOCK_COMMENT) { // block comment must never be formatted
            return new LeafBlock(node, formatter, null, null, ABSOLLUTE_NONE_INDENT);
        }

        if (node instanceof LeafPsiElement) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.MODE) {
            return new ModeBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.LAMBDA_EXPR) {
            return new LambdaBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.ASSIGNMENT_EXPR || type == QTypes.QUERY_COLUMN || type == QTypes.TYPED_PARAMETER) {
            return new AssignmentBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.ARGUMENTS) {
            return BracketsBlock.arguments(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.PARENTHESES_EXPR) {
            return BracketsBlock.parentheses(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.GROUPING_EXPR) {
            return BracketsBlock.grouping(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.CONDITION_EXPR || type == QTypes.CONTROL_EXPR) {
            return new ControlBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.QUERY_EXPR) {
            return new QueryBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.TABLE_EXPR) {
            return new TableBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.PATTERN_PARAMETER) {
            return BracketsBlock.parentheses(node, formatter, wrap, alignment, indent);
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
        return type == QTypes.VAR_DECLARATION
                || type == QTypes.VAR_REFERENCE
                || type == QTypes.VAR_ASSIGNMENT_TYPE
                || type == QTypes.COLUMN_ASSIGNMENT_TYPE
                || type == QTypes.SYMBOL
                || type == QTypes.SYMBOLS
                || type == QTypes.ITERATOR_TYPE
                ;
    }

    private boolean isWrapperType(IElementType type) {
        return type == QTypes.LITERAL_EXPR
                || type == QTypes.OPERATOR_TYPE
                || type == QTypes.SYSTEM_FUNCTION
                || type == QTypes.INTERNAL_FUNCTION
                ;
    }

    private boolean isExpressionType(IElementType type) {
        return type == QTypes.EXPRESSIONS
                || type == QTypes.VAR_ACCUMULATOR_TYPE
                || type == QTypes.CUSTOM_FUNCTION
                || type == QTypes.SIGNAL_EXPR
                || type == QTypes.RETURN_EXPR
                || type == QTypes.CONTEXT
                || type == QTypes.CONTEXT_BODY
                || type == QTypes.VAR_INDEXING
                || type == QTypes.COMMAND
                || type == QTypes.IMPORT_COMMAND
                || type == QTypes.IMPORT_FUNCTION
                || type == QTypes.K_SYNTAX_EXPR
                || type == QTypes.PROJECTION_EXPR
                || type == QTypes.TYPE_CAST_EXPR
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
