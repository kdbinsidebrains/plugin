package org.kdb.inside.brains.psi;

public abstract class QLexerBase implements com.intellij.lexer.FlexLexer {
    private boolean lambdaParsing = false;
    private boolean queryParsing = false;

    public void beginQuery() {
        queryParsing = true;
    }

    public void finishQuery() {
        queryParsing = false;
    }

    public void beginLambda() {
        lambdaParsing = true;
    }

    public void finishLambda() {
        lambdaParsing = false;
    }

    public boolean isQuerySplitter() {
        return queryParsing && !lambdaParsing;
    }
}
