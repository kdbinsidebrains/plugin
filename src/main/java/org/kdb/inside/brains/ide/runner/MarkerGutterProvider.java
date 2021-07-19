package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.console.GutterContentProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * This implementation has copy/paste issue when the marker is inserted into copying test.
 * <p>
 * At this moment I'm testing {@link LineNumberGutterProvider} instead that keep a line number in internal cache.
 * <p>
 * If this class is deprecated for a log time - it must be just removed from the source code. New one is the best.
 */
@Deprecated
public class MarkerGutterProvider extends GutterContentProvider {
    public static final String MARKER = "\u200C";

    public MarkerGutterProvider() {
    }

    @Override
    public boolean hasText() {
        return false;
    }

    @Nullable
    @Override
    public String getText(int line, @NotNull Editor editor) {
        return null;
    }

    @Nullable
    @Override
    public String getToolTip(int line, @NotNull Editor editor) {
        return null;
    }

    @Override
    public void doAction(int line, @NotNull Editor editor) {
    }

    @Override
    public void beforeEvaluate(@NotNull Editor editor) {
        final Document document = editor.getDocument();
        document.insertString(document.getTextLength(), MARKER);
    }

    @Override
    public boolean drawIcon(int line, @NotNull Graphics g, int y, @NotNull Editor editor) {
        if (isInputLine(line, editor)) {
            g.setColor(JBColor.GRAY);
            g.drawString(">", 3, y);
            return true;
        }
        return false;
    }

    @Override
    public boolean isShowSeparatorLine(int line, @NotNull Editor editor) {
        Document document = editor.getDocument();
        final int i = line + 1;
        if (i >= document.getLineCount()) {
            return true;
        }
        return isInputLine(i, editor);
    }

    private boolean isInputLine(int line, @NotNull Editor editor) {
        final Document document = editor.getDocument();
        if (line >= document.getLineCount()) {
            return false;
        }

        final int lineStartOffset = document.getLineStartOffset(line);
        if (document.getTextLength() == lineStartOffset) {
            return false;
        }

        final char c = document.getImmutableCharSequence().charAt(lineStartOffset);
        return c == MARKER.charAt(0);
    }
}