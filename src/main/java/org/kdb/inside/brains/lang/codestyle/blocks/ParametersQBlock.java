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
import java.util.function.Function;

public class ParametersQBlock extends AbstractQBlock {
    public ParametersQBlock(@NotNull ASTNode node,
                            @NotNull QSpacingStrategy spacingStrategy,
                            @NotNull QCodeStyleSettings qSettings,
                            @NotNull CommonCodeStyleSettings settings,
                            @Nullable Wrap wrap,
                            @Nullable Indent indent,
                            @Nullable Alignment alignment) {
        super(node, spacingStrategy, qSettings, settings, wrap, indent, alignment);
    }

    @Override
    protected List<Block> buildChildren() {
        final Indent indent = Indent.getNormalIndent(false);
        final boolean align = settings.ALIGN_MULTILINE_PARAMETERS;
        final Wrap wrap = Wrap.createWrap(settings.METHOD_PARAMETERS_WRAP, false);

        return buildChildren(myNode, new Function<>() {
            Alignment params = null;
            Alignment bracket = null;

            @Override
            public Block apply(ASTNode child) {
                final IElementType elementType = child.getElementType();
                if (elementType == QTypes.VAR_DECLARATION || elementType == QTypes.SEMICOLON) {
                    final Alignment a = align ? (params == null ? params = Alignment.createChildAlignment(bracket) : Alignment.createChildAlignment(params)) : null;
                    return createBlock(child, elementType == QTypes.VAR_DECLARATION ? wrap : null, indent, a);
                } else if (elementType == QTypes.BRACKET_OPEN || elementType == QTypes.BRACKET_CLOSE) {
                    final Alignment a = bracket == null ? bracket = Alignment.createAlignment() : Alignment.createChildAlignment(bracket);
                    return createBlock(child, null, indent, a);
                }
                return null;
            }
        });
    }
}
