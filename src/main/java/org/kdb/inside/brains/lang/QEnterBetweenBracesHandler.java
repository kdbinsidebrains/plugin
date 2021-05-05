package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate;

public class QEnterBetweenBracesHandler extends EnterBetweenBracesDelegate {
    @Override
    protected boolean isBracePair(char lBrace, char rBrace) {
        return super.isBracePair(lBrace, rBrace) || (lBrace == '[' && rBrace == ']');
    }
}
