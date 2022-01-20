package org.kdb.inside.brains.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.NavigatablePsiElement;

public interface QImport extends QPsiElement, NavigatablePsiElement {
    default TextRange getFilePathRange() {
        final String text = getText();
        final int end = text.length() - 1;

        final int l = text.indexOf('l');
        int s = l + 2;
        if (s > end) {
            return TextRange.EMPTY_RANGE;
        }

        while (s <= end && Character.isWhitespace(text.charAt(s))) {
            s++;
        }
        if (s > end) {
            return TextRange.EMPTY_RANGE;
        }

        int e = text.lastIndexOf('"') - 1;
        if (e < 0) {
            e = end;
        }
        while (Character.isWhitespace(text.charAt(e))) {
            e--;
        }
        if (e == l || e + 1 <= s) {
            return TextRange.EMPTY_RANGE;
        }
        return new TextRange(s, e + 1);
    }

    default String getFilePath() {
        return getFilePathRange().substring(getText());
    }
}
