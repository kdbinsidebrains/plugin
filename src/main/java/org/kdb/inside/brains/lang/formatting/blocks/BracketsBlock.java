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
import java.util.function.Function;

public class BracketsBlock extends AbstractQBlock {
    private final Descriptor descriptor;

    private static final Descriptor GROUPING = new Descriptor(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, c -> c.GROUPING_WRAP, c -> c.GROUPING_ALIGN_BRACKET, c -> c.GROUPING_ALIGN_EXPRS);
    private static final Descriptor ARGUMENTS = new Descriptor(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, c -> c.ARGUMENTS_WRAP, c -> c.ARGUMENTS_ALIGN_BRACKET, c -> c.ARGUMENTS_ALIGN_EXPRS);
    private static final Descriptor PARAMETERS = new Descriptor(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, c -> c.LAMBDA_PARAMS_WRAP, c -> c.LAMBDA_PARAMS_ALIGN_BRACKETS, c -> c.LAMBDA_PARAMS_ALIGN_NAMES);
    private static final Descriptor PARENTHESES = new Descriptor(QTypes.PAREN_OPEN, QTypes.PAREN_CLOSE, c -> c.PARENTHESES_WRAP, c -> c.PARENTHESES_ALIGN_PAREN, c -> c.PARENTHESES_ALIGN_EXPRS);

    private BracketsBlock(@NotNull Descriptor descriptor, @NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        super(node, formatter, wrap, alignment, indent);
        this.descriptor = descriptor;
    }

    public static BracketsBlock grouping(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        return new BracketsBlock(GROUPING, node, formatter, wrap, alignment, indent);
    }

    public static BracketsBlock arguments(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        return new BracketsBlock(ARGUMENTS, node, formatter, wrap, alignment, indent);
    }

    public static BracketsBlock parameters(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        return new BracketsBlock(PARAMETERS, node, formatter, wrap, alignment, indent);
    }

    public static BracketsBlock parentheses(@NotNull ASTNode node, @NotNull QFormatter formatter, @Nullable Wrap wrap, @Nullable Alignment alignment, @NotNull Indent indent) {
        return new BracketsBlock(PARENTHESES, node, formatter, wrap, alignment, indent);
    }

    @Override
    protected Indent getChildIndent() {
        return NORMAL_INDENT;
    }

    @Override
    protected List<Block> buildChildren() {
        final QCodeStyleSettings custom = formatter.custom;

        final Wrap wrap = Wrap.createWrap(descriptor.wrap.apply(custom), false);
        final Alignment bracketAlignment = descriptor.alignBrac.apply(custom) ? Alignment.createAlignment() : null;
        final Alignment expressionAlignment = descriptor.alignExpr.apply(custom) ? Alignment.createAlignment() : null;

        return iterateChildren((node, first) -> {
            final IElementType type = node.getElementType();
            if (type == descriptor.open) {
                return new LeafBlock(node, formatter, null, bracketAlignment, NONE_INDENT);
            }
            if (type == descriptor.close) {
                return new LeafBlock(node, formatter, null, bracketAlignment, NORMAL_INDENT);
            }
            if (type == QTypes.SEMICOLON) {
                return new LeafBlock(node, formatter, null, expressionAlignment, NORMAL_INDENT);
            }
            return createBlock(node, formatter, wrap, expressionAlignment, NORMAL_INDENT);
        });
    }

    private record Descriptor(IElementType open, IElementType close, Function<QCodeStyleSettings, Integer> wrap,
                              Function<QCodeStyleSettings, Boolean> alignBrac,
                              Function<QCodeStyleSettings, Boolean> alignExpr) {
    }
}
