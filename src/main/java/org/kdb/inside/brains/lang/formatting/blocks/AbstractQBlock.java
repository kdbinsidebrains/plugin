package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.QTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractQBlock extends AbstractBlock {
    protected static final Indent NONE_INDENT = Indent.getNoneIndent();
    protected static final Indent NORMAL_INDENT = Indent.getNormalIndent();
    protected static final Indent SPACE_INDENT = Indent.getSpaceIndent(1);
    protected final QFormatter formatter;
    private final Indent indent;

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QFormatter formatter) {
        this(node, formatter, null, null, NONE_INDENT);
    }

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QFormatter formatter,
                          @NotNull Indent indent) {
        this(node, formatter, null, null, indent);
    }

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QFormatter formatter,
                          @Nullable Wrap wrap,
                          @Nullable Alignment alignment) {
        this(node, formatter, wrap, alignment, NONE_INDENT);
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
        return createBlock(node, formatter, null, null, NONE_INDENT);
    }

    protected @Nullable Block createBlock(@Nullable ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @Nullable Indent indent) {
        if (node == null) {
            return null;
        }

        final IElementType type = node.getElementType();
        if (type == QTypes.NEW_LINE) {
            return null;
        }

        if (node instanceof LeafPsiElement) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.LAMBDA_EXPR) {
            return new LambdaBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.ASSIGNMENT_EXPR) {
            System.out.println(">>>>>>>>>> " + indent);
            return new AssignmentBlock(node, formatter, wrap, alignment, indent);
        }

        if (type == QTypes.CONDITION_EXPR || type == QTypes.CONTROL_EXPR) {
            return new ControlBlock(node, formatter);
        }

        // One value wrapper type which should be expanded
        if (type == QTypes.INVOKE_OPERATOR || type == QTypes.INVOKE_FUNCTION) {
            return createBlock(getFirstNotEmptyChild(node), formatter);
        }

        if (isExpressionType(type)) {
            return new CodeBlock(node, formatter);
        }

        if (isPrimitiveType(type)) {
            return new LeafBlock(node, formatter, wrap, alignment, indent);
        }

        System.out.println("Leaf type created: " + type);
        return new LeafBlock(node, formatter, wrap, alignment, indent);
    }

    private boolean isPrimitiveType(IElementType type) {
        return type == QTypes.VAR_DECLARATION
                || type == QTypes.VAR_REFERENCE
                || type == QTypes.ASSIGNMENT_TYPE;
    }

    private boolean isExpressionType(IElementType type) {
        return type == QTypes.ASSIGNMENT_EXPR
                || type == QTypes.FUNCTION_INVOKE_EXPR;
/*
                || type == QTypes.EXPRESSION
                || type == QTypes.GROUPING_EXPR
                || type == QTypes.K_SYNTAX_EXPR
                || type == QTypes.LAMBDA_EXPR
                || type == QTypes.LITERAL_EXPR
                || type == QTypes.PARENTHESES_EXPR
                || type == QTypes.PARENTHESES_INVOKE_EXPR
                || type == QTypes.PREFIX_INVOKE_EXPR
                || type == QTypes.PROJECTION_EXPR
                || type == QTypes.QUERY_EXPR
                || type == QTypes.RETURN_EXPR
                || type == QTypes.SIGNAL_EXPR
                || type == QTypes.TABLE_EXPR
                || type == QTypes.TYPE_CAST_EXPR;
*/
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return formatter.getSpacing(this, child1, child2);
    }

    protected List<Block> iterateFromChild(ASTNode child, Function<ASTNode, Block> consumer) {
        final List<Block> result = new ArrayList<>();
        while (child != null) {
            final Block block = consumer.apply(child);
            if (block != null) {
                result.add(block);
            }
            child = getNextNotEmptySibling(child);
        }
        return result;
    }

    protected final List<Block> iterateChildren(ASTNode node, Function<ASTNode, Block> consumer) {
        return iterateFromChild(getFirstNotEmptyChild(node), consumer);
    }

    protected final List<Block> iterateSiblings(ASTNode currentNode, Function<ASTNode, Block> consumer) {
        return iterateFromChild(getNextNotEmptySibling(currentNode), consumer);
    }

    protected ASTNode getFirstNotEmptyChild(@NotNull ASTNode node) {
        ASTNode res = node.getFirstChildNode();
        while (res != null && isEmptyNode(res)) {
            res = res.getTreeNext();
        }
        return res;
    }

    protected ASTNode getNextNotEmptySibling(@NotNull ASTNode node) {
        ASTNode res = node.getTreeNext();
        while (res != null && isEmptyNode(res)) {
            res = res.getTreeNext();
        }
        return res;
    }

    protected boolean isEmptyNode(@NotNull ASTNode child) {
        return FormatterUtil.containsWhiteSpacesOnly(child) || child.getTextLength() <= 0;
    }
}
