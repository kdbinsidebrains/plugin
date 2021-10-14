package org.kdb.inside.brains.lang.codestyle.blocks;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.codestyle.QCodeStyleSettings;
import org.kdb.inside.brains.lang.codestyle.QSpacingStrategy;

import java.util.List;

public class ModeQBlock extends AbstractQBlock {
    public ModeQBlock(@NotNull ASTNode node,
                      @NotNull QSpacingStrategy spacingBuilder,
                      @NotNull QCodeStyleSettings qSettings,
                      @NotNull CommonCodeStyleSettings settings,
                      @Nullable Wrap wrap,
                      @Nullable Indent indent,
                      @Nullable Alignment alignment) {
        super(node, spacingBuilder, qSettings, settings, wrap, indent, alignment);
    }

    @Override
    protected List<Block> buildChildren() {
        return List.of();
    }
}
