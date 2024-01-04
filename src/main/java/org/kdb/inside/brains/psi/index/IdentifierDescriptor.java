package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.util.TextRange;

import java.util.List;

public record IdentifierDescriptor(IdentifierType type, List<String> params, TextRange range) {
    public boolean isSymbol() {
        return type == IdentifierType.SYMBOL;
    }

    public boolean isVariable() {
        return !isSymbol();
    }

    @Override
    public String toString() {
        return "IdentifierDescriptor{" +
                "type=" + type +
                ", range=" + range +
                '}';
    }
}
