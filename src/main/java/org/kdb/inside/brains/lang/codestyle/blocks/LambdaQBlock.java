package org.kdb.inside.brains.lang.codestyle.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.codestyle.QCodeStyleSettings;
import org.kdb.inside.brains.lang.codestyle.QSpacingStrategy;
import org.kdb.inside.brains.psi.ElementContext;
import org.kdb.inside.brains.psi.QTypes;

public class LambdaQBlock extends AbstractQBlock {
    public LambdaQBlock(@NotNull ASTNode node,
                        @NotNull QSpacingStrategy spacingBuilder,
                        @NotNull QCodeStyleSettings qSettings,
                        @NotNull CommonCodeStyleSettings settings,
                        @Nullable Wrap wrap,
                        @Nullable Indent indent,
                        @Nullable Alignment alignment) {
        super(node, spacingBuilder, qSettings, settings, wrap, indent, alignment);
    }

    @Override
    protected @Nullable Indent createChildIndent(ASTNode child) {
        final IElementType elementType = child.getElementType();

        if (elementType == QTypes.CODE_BLOCK) {
            return Indent.getNormalIndent(false);
        }

        if (qSettings.SPACE_BEFORE_BRACE_CLOSE && elementType == QTypes.BRACE_CLOSE && ElementContext.of(myNode) == null) {
            return Indent.getSpaceIndent(1);
        }
        return null;
    }
}
