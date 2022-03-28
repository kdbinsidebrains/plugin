package org.kdb.inside.brains.lang;

import com.intellij.lang.DefaultASTFactoryImpl;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QTypes;

public class QASTFactory extends DefaultASTFactoryImpl {
    @Override
    public @NotNull LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
        // Special case for new line: it's whitespace but must be processed for parser
        if (type == QTypes.NEW_LINE) {
            return whitespace(text);
        }
        return super.createLeaf(type, text);
    }
}
