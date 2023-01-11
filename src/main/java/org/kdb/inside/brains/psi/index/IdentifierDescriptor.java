package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.util.TextRange;

import java.util.List;
import java.util.Objects;

public class IdentifierDescriptor {
    private final IdentifierType type;
    private final List<String> params;
    private final TextRange range;

    public IdentifierDescriptor(IdentifierType type, List<String> params, TextRange range) {
        this.type = type;
        this.params = params;
        this.range = range;
    }

    public TextRange getRange() {
        return range;
    }

    public IdentifierType getType() {
        return type;
    }

    public List<String> getParams() {
        return params;
    }

    public boolean isSymbol() {
        return type == IdentifierType.SYMBOL;
    }

    public boolean isVariable() {
        return !isSymbol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifierDescriptor that = (IdentifierDescriptor) o;
        return type == that.type && Objects.equals(params, that.params) && Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, params, range);
    }

    @Override
    public String toString() {
        return "IdentifierDescriptor{" +
                "type=" + type +
                ", range=" + range +
                '}';
    }
}
