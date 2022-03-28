package org.kdb.inside.brains.lang.formatting.blocks.old;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.QFormatter;
import org.kdb.inside.brains.lang.formatting.blocks.AbstractQBlock;
import org.kdb.inside.brains.psi.QTypes;

import java.util.List;
import java.util.function.Function;

@Deprecated
public class TableQBlock extends AbstractQBlock {
    public TableQBlock(@NotNull ASTNode node,
                       @NotNull QFormatter formatter,
                       @Nullable Wrap wrap,
                       @Nullable Indent indent,
                       @Nullable Alignment alignment) {
        super(node, formatter, wrap, alignment, indent);
    }

    @Override
    protected List<Block> buildChildren() {
        final boolean multiline = formatter.custom.TABLE_WRAP_TYPE != CommonCodeStyleSettings.DO_NOT_WRAP || myNode.getTreeParent().getText().indexOf('\n') >= 0;

        return iterateChildren(myNode, new Function<>() {
            final Alignment rootAlign = Alignment.createAlignment();
            final Alignment childAlign = formatter.custom.TABLE_WRAP_ALIGN ? Alignment.createChildAlignment(rootAlign) : null;

            final Wrap keyWrap = Wrap.createWrap(formatter.custom.TABLE_WRAP_TYPE, false);
            final Wrap valueWrap = Wrap.createWrap(formatter.custom.TABLE_WRAP_TYPE, true);

            @Override
            public Block apply(ASTNode node) {
                final IElementType type = node.getElementType();
/*
                if (type == QTypes.TABLE_KEYS || type == QTypes.TABLE_VALUES) {
                    return iterateChildren(node, this);
                }
*/

                final Wrap wrap = getWrap(type, node);
                final Indent normalIndent = Indent.getNormalIndent(false);
                final Alignment alignment = getAlignment(type);

//                return createBlock(node, wrap, normalIndent, alignment);
                return null;
            }

            @Nullable
            private Wrap getWrap(IElementType type, ASTNode node) {
                if (multiline) {
                    if ((type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE) && formatter.custom.TABLE_KEYS_EMPTY_LINE) {
                        return Wrap.createWrap(WrapType.ALWAYS, true);
                    }
                    if (type == QTypes.PAREN_CLOSE && formatter.custom.TABLE_CPAREN_EMPTY_LINE) {
                        return Wrap.createWrap(WrapType.ALWAYS, true);
                    }
                }

                if (type == QTypes.TABLE_COLUMN) {
                    final IElementType elementType = node.getTreeParent().getElementType();
                    if (elementType == QTypes.TABLE_KEYS) {
                        if (multiline && formatter.custom.TABLE_KEYS_EMPTY_LINE) {
                            return Wrap.createWrap(WrapType.ALWAYS, true);
                        } else {
                            return keyWrap;
                        }
                    }
                    return valueWrap;
                }
                return null;
            }

            private Alignment getAlignment(IElementType type) {
                return type == QTypes.PAREN_OPEN ? rootAlign : childAlign;
            }
        });
    }

}
