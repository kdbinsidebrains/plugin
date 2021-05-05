package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QTypes;

public final class QBracesMatcher extends PairedBraceMatcherAdapter {
    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(QTypes.BRACE_OPEN, QTypes.BRACE_CLOSE, true),
            new BracePair(QTypes.PAREN_OPEN, QTypes.PAREN_CLOSE, true),
            new BracePair(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, true),
    };

    public QBracesMatcher() {
        super(new PairedBraceMatcher() {
            @Override
            public BracePair @NotNull [] getPairs() {
                return PAIRS;
            }

            @Override
            public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
                return true;
            }

            @Override
            public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
                return openingBraceOffset;
            }
        }, QLanguage.INSTANCE);
    }
}
