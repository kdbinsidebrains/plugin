package org.kdb.inside.brains.lang.formatting.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.psi.ElementContext;

import java.util.List;

import static org.kdb.inside.brains.psi.QTypes.*;

public class InvokeBlock extends AbstractQBlock {
    private static final TokenSet TOKEN_SET = TokenSet.create(OPERATOR_TYPE, ITERATOR_TYPE);
    private final Alignment rootAlignment;

    public InvokeBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
        this.rootAlignment = null;
    }

    public InvokeBlock(@NotNull ASTNode node, @NotNull QFormatter formatter, @NotNull Indent indent, Alignment rootAlignment) {
        super(node, formatter, null, null, indent);
        this.rootAlignment = rootAlignment;
    }

    public static boolean isInvokeElement(ASTNode node) {
        return isInvokeElement(node.getElementType());
    }

    public static boolean isInvokeElement(IElementType type) {
        return type == INVOKE_FUNCTION || type == INVOKE_PREFIX || type == INVOKE_PARENTHESES;
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final Indent indent = getChildrenIndent();
        final Alignment alignment = getChildrenAlign();

        return iterateChildren((node, first) -> {
            if (first) {
                if (ElementContext.isRoot(myNode) || notOperationOrIterator(myNode)) {
                    return createBlock(node, formatter, null, null, NONE_INDENT);
                } else {
                    return createBlock(node, formatter, null, alignment, indent);
                }
            }

            if (isInvokeElement(node)) {
                if (notOperationOrIterator(node)) {
                    return new InvokeBlock(node, formatter, null, alignment, indent);
                } else {
                    return new InvokeBlock(node, formatter, NONE_INDENT, alignment);
                }
            }
            return createBlock(node, formatter, null, alignment, indent);
        });
    }

    private boolean notOperationOrIterator(ASTNode node) {
        return node.findChildByType(TOKEN_SET) == null;
    }

    private Indent getChildrenIndent() {
        return isInsideGroup() ? NONE_INDENT : NORMAL_INDENT;
    }

    private boolean isInsideGroup() {
        ASTNode node = myNode;
        while (isInvokeElement(node)) {
            if (notOperationOrIterator(node)) {
                return false;
            }
            node = node.getTreeParent();
        }
        final IElementType type = node.getElementType();
        return type == ARGUMENTS || type == EXPRESSIONS || type == PARENTHESES_EXPR || type == GROUPING_EXPR;
    }

    @Nullable
    private Alignment getChildrenAlign() {
        if (formatter.custom.INVOKE_ALIGN_ITEMS) {
            return rootAlignment == null ? Alignment.createAlignment() : rootAlignment;
        }
        return null;
    }
}