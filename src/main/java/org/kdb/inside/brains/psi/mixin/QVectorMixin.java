package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.kdb.inside.brains.psi.QPsiElementImpl;
import org.kdb.inside.brains.psi.QVector;

public class QVectorMixin extends QPsiElementImpl implements QVector {
    public QVectorMixin(ASTNode node) {
        super(node);
    }

    @Override
    public int getIndexForPosition(int globalPosition) {
        final String text = getText();
        final TextRange range = getTextRange();

        final int localOffset = globalPosition - range.getStartOffset();

        if (text.startsWith("0x")) {
            int dataOffset = Math.max(0, localOffset - 2);
            String hex = text.substring(2);
            if (hex.length() % 2 != 0) {
                return (dataOffset == 0) ? 0 : ((dataOffset - 1) / 2) + 1;
            }
            return dataOffset / 2;
        }

        if (text.endsWith("b")) {
            return Math.min(localOffset, text.length() - 2);
        }

        int effectiveLength = text.length();
        if (effectiveLength > 0 && Character.isLetter(text.charAt(effectiveLength - 1))) {
            effectiveLength--;
        }
        if (localOffset >= effectiveLength) {
            return -1;
        }

        int index = 0;
        boolean inWord = false;
        for (int i = 0; i < localOffset; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (inWord) {
                    index++;
                    inWord = false;
                }
            } else {
                inWord = true;
            }
        }
        return index;
    }

    @Override
    public TextRange getRangeForIndex(int index) {
        int startOffset;
        int endOffset;
        String text = getText();

        if (text.startsWith("0x")) {
            // Case A: Bytes (0x...)
            String hexData = text.substring(2);
            boolean isOdd = hexData.length() % 2 != 0;

            if (isOdd) {
                // First byte is 1 char, others are 2
                startOffset = (index == 0) ? 2 : 2 + 1 + (index - 1) * 2;
                endOffset = (index == 0) ? 3 : startOffset + 2;
            } else {
                // All bytes are 2 chars
                startOffset = 2 + (index * 2);
                endOffset = startOffset + 2;
            }
        } else if (text.endsWith("b")) {
            // Case B: Booleans (...b)
            startOffset = index;
            endOffset = index + 1;
        } else {
            int effectiveLength = text.length();
            if (effectiveLength > 0 && Character.isLetter(text.charAt(effectiveLength - 1))) {
                effectiveLength--;
            }

            int currentIndex = 0;
            int i = 0;

            // Skip leading whitespace to find the first word
            while (i < effectiveLength && Character.isWhitespace(text.charAt(i))) {
                i++;
            }

            while (i < effectiveLength && currentIndex < index) {
                // Skip the current word
                while (i < effectiveLength && !Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                // Skip the following whitespace to get to the start of the next word
                while (i < effectiveLength && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                currentIndex++;
            }

            startOffset = i;
            while (i < effectiveLength && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            endOffset = i;
        }
        return new TextRange(startOffset, endOffset).shiftRight(getTextRange().getStartOffset());
    }
}
