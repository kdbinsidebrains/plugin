package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.kdb.inside.brains.psi.QImportFile;

public class QImportCommandBase extends AbstractQImportBase {
    public QImportCommandBase(ASTNode node) {
        super(node);
    }

    @Override
    public TextRange getFilepathRange() {
        final QImportFile childByClass = findChildByClass(QImportFile.class);
        if (childByClass != null) {
            return childByClass.getTextRangeInParent();
        }
        return null;
    }
}