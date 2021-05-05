package org.kdb.inside.brains.psi;

import com.intellij.psi.tree.IElementType;
import org.kdb.inside.brains.QLanguage;

public final class QTokenType extends IElementType {
    public QTokenType(String debugName) {
        super(debugName, QLanguage.INSTANCE);
    }
}
