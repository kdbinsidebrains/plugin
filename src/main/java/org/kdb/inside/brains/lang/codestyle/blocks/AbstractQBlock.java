package org.kdb.inside.brains.lang.codestyle.blocks;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.codestyle.QCodeStyleSettings;
import org.kdb.inside.brains.lang.codestyle.QSpacingStrategy;
import org.kdb.inside.brains.psi.QTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractQBlock extends AbstractBlock {
    protected final Indent indent;
    protected final QCodeStyleSettings qSettings;
    protected final CommonCodeStyleSettings settings;
    protected final QSpacingStrategy spacingStrategy;

    public AbstractQBlock(@NotNull ASTNode node,
                          @NotNull QSpacingStrategy spacingStrategy,
                          @NotNull QCodeStyleSettings qSettings,
                          @NotNull CommonCodeStyleSettings settings,
                          @Nullable Wrap wrap,
                          @Nullable Indent indent,
                          @Nullable Alignment alignment) {
        super(node, wrap, alignment);
        this.settings = settings;
        this.qSettings = qSettings;
        this.indent = indent;
        this.spacingStrategy = spacingStrategy;
    }

    @Override
    protected List<Block> buildChildren() {
        final Alignment childrenAlignment = createChildrenAlignment();
        return iterateNotEmptyChildren(myNode, child -> {
            final Wrap wrap = createChildWrap(child);
            final Indent indent = createChildIndent(child);
            return createBlock(child, wrap, indent, childrenAlignment);
        });
    }

    protected final List<Block> iterateNotEmptyChildren(ASTNode node, Function<ASTNode, Block> consumer) {
        return iterateNotEmptyListChildren(node, child -> List.of(consumer.apply(child)));
    }

    protected final List<Block> iterateNotEmptyListChildren(ASTNode node, Function<ASTNode, List<Block>> consumer) {
        final List<Block> result = new ArrayList<>();
        ASTNode child = getFirstNotEmptyChild(node);
        while (child != null) {
            final List<Block> apply = consumer.apply(child);
            if (apply != null && !apply.isEmpty()) {
                result.addAll(apply);
            }
            child = getNextNotEmptySubling(child);
        }
        return result;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return spacingStrategy.getSpacing(this, child1, child2);
    }

    @NotNull
    protected Block createBlock(@NotNull ASTNode node,
                                @Nullable Wrap wrap,
                                @Nullable Indent indent,
                                @Nullable Alignment alignment) {

        final Indent actualIndent = indent == null ? getDefaultIndent(node) : indent;
        IElementType elementType = node.getElementType();

        if (elementType == QTypes.LAMBDA_EXPR) {
            return new LambdaQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
        }

        if (elementType == QTypes.PARAMETERS) {
            return new ParametersQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
        }

        if (elementType == QTypes.QUERY_EXPR) {
            return new QueryQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
        }

        if (elementType == QTypes.TABLE_EXPR) {
            return new TableQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
        }

        if (elementType == QTypes.MODE) {
            return new ModeQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
        }

        if (elementType == QTypes.OPERATION) {
            final ASTNode firstChildNode = getFirstNotEmptyChild(node);
            if (firstChildNode != null) {
                if (firstChildNode.getElementType() == QTypes.OPERATOR_TYPE) {
                    final ASTNode cnd = getFirstNotEmptyChild(firstChildNode);
                    if (cnd != null) {
                        return new SimpleQBlock(cnd, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
                    }
                }
                return new SimpleQBlock(firstChildNode, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
            }
        }

        if (elementType == QTypes.CONTROL_EXPR || elementType == QTypes.CONDITION_EXPR) {
            return new ConditionControlQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment, elementType);
        }

        return new SimpleQBlock(node, spacingStrategy, qSettings, settings, wrap, actualIndent, alignment);
    }


    @Nullable
    protected Indent getDefaultIndent(@NotNull ASTNode child) {
        return Indent.getNoneIndent();
    }

    @Nullable
    protected Wrap createChildWrap(ASTNode child) {
        return null;
    }

    @Nullable
    protected Indent createChildIndent(ASTNode child) {
        return null;
    }

    protected Alignment createChildrenAlignment() {
        return null;
    }

    @Nullable
    protected ASTNode getFirstNotEmptyChild(@NotNull ASTNode node) {
        ASTNode res = node.getFirstChildNode();
        while (res != null && isEmptyNode(res)) {
            res = res.getTreeNext();
        }
        return res;
    }

    protected ASTNode getNextNotEmptySubling(@NotNull ASTNode node) {
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
