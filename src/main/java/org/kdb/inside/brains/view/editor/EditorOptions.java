package org.kdb.inside.brains.view.editor;

import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class EditorOptions implements SettingsBean<EditorOptions> {
    private boolean rainbowBrace = true;
    private boolean rainbowParen = true;
    private boolean rainbowBracket = true;
    private boolean rainbowVariables = true;
    private boolean highlightVector = true;

    public boolean isHighlightVector() {
        return highlightVector;
    }

    public void setHighlightVector(boolean highlightVector) {
        this.highlightVector = highlightVector;
    }

    public boolean isRainbowBrace() {
        return rainbowBrace;
    }

    public void setRainbowBrace(boolean rainbowBrace) {
        this.rainbowBrace = rainbowBrace;
    }

    public boolean isRainbowBracket() {
        return rainbowBracket;
    }

    public void setRainbowBracket(boolean rainbowBracket) {
        this.rainbowBracket = rainbowBracket;
    }

    public boolean isRainbowParen() {
        return rainbowParen;
    }

    public void setRainbowParen(boolean rainbowParen) {
        this.rainbowParen = rainbowParen;
    }

    public boolean isRainbowVariables() {
        return rainbowVariables;
    }

    public void setRainbowVariables(boolean rainbowVariables) {
        this.rainbowVariables = rainbowVariables;
    }

    @Override
    public void copyFrom(EditorOptions o) {
        this.rainbowBrace = o.rainbowBrace;
        this.rainbowParen = o.rainbowParen;
        this.rainbowBracket = o.rainbowBracket;
        this.rainbowVariables = o.rainbowVariables;
        this.highlightVector = o.highlightVector;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EditorOptions that = (EditorOptions) o;
        return rainbowBrace == that.rainbowBrace && rainbowParen == that.rainbowParen && rainbowBracket == that.rainbowBracket && rainbowVariables == that.rainbowVariables && highlightVector == that.highlightVector;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rainbowBrace, rainbowParen, rainbowBracket, rainbowVariables, highlightVector);
    }
}