package org.kdb.inside.brains;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;

import java.io.Reader;

public class QLexer extends _QLexer {
    public QLexer() {
        super(null);
    }

    public QLexer(Reader in) {
        super(in);
    }

    @Override
    public void reset(CharSequence buffer, int start, int end, int initialState) {
        super.reset(buffer, start, end, initialState);
        resetState();
    }

    public static Lexer newLexer() {
        return new FlexAdapter(new QLexer());
    }
}
