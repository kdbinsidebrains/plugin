package org.kdb.inside.brains.view;

import com.intellij.execution.console.GutterContentProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;


public class LineNumberGutterProvider extends GutterContentProvider {
    private final Set<Integer> markersPosition = new HashSet<>();

    public LineNumberGutterProvider() {
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
        markersPosition.add(document.getLineCount() - 1);
    }

    @Override
    public void documentCleared(@NotNull Editor editor) {
        super.documentCleared(editor);
        markersPosition.clear();
        markersPosition.add(0);
    }

    @Override
    public boolean drawIcon(int line, @NotNull Graphics g, int y, @NotNull Editor editor) {
        if (markersPosition.contains(line)) {
            g.setColor(JBColor.GRAY);
            g.drawString(">", 0, y);
            return true;
        }
        return false;
    }

    @Override
    public boolean isShowSeparatorLine(int line, @NotNull Editor editor) {
        return markersPosition.contains(line + 1);
    }
}