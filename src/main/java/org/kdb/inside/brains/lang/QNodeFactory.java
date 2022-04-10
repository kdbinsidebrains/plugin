package org.kdb.inside.brains.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.DefaultASTFactoryImpl;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QTypes;

public class QNodeFactory extends DefaultASTFactoryImpl {
    public static @Nullable ASTNode getFirstNotEmptyChild(@Nullable ASTNode node) {
        if (node == null) {
            return null;
        }
        ASTNode res = node.getFirstChildNode();
        while (res != null && isEmptyNode(res)) {
            res = res.getTreeNext();
        }
        return res;
    }

    public static @Nullable ASTNode getNextNotEmptySibling(@Nullable ASTNode node) {
        if (node == null) {
            return null;
        }
        ASTNode res = node.getTreeNext();
        while (res != null && isEmptyNode(res)) {
            res = res.getTreeNext();
        }
        return res;
    }

    public static boolean isEmptyNode(@NotNull ASTNode child) {
        return FormatterUtil.containsWhiteSpacesOnly(child) || child.getTextLength() <= 0;
    }

    @Override
    public @NotNull LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
        // Special case for new line: it's whitespace but must be processed for parser
        if (type == QTypes.NEW_LINE) {
            return whitespace(text);
        }
        return super.createLeaf(type, text);
    }
}
