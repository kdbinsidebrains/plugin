package org.kdb.inside.brains.lang.usage;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import org.kdb.inside.brains.QLexer;
import org.kdb.inside.brains.QParserDefinition;
import org.kdb.inside.brains.psi.QTypes;

public class QWordsScanner extends DefaultWordsScanner {
    public QWordsScanner() {
        super(QLexer.newLexer(), TokenSet.create(QTypes.VARIABLE, QTypes.SYMBOL), QParserDefinition.COMMENTS, TokenSet.EMPTY);
        setMayHaveFileRefsInLiterals(true);
    }
}
