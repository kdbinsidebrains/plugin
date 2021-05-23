package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.kdb.inside.brains.psi.QImport;

public class QImportFunctionBase extends QPsiElementImpl implements QImport {
    public QImportFunctionBase(ASTNode node) {
        super(node);
    }

    @Override
    public TextRange getFilepathRange() {
        final String text = getText();
        int s = text.indexOf('l') + 2;
        while (Character.isWhitespace(text.charAt(s))) {
            s++;
        }
        int e = text.lastIndexOf('"');
        while (Character.isWhitespace(text.charAt(e))) {
            e--;
        }
        return new TextRange(s, e);
    }
}
