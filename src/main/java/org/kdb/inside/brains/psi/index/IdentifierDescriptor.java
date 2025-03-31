package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.util.TextRange;

public record IdentifierDescriptor(IdentifierType type, TextRange range) {
    public boolean isSymbol() {
        return type == IdentifierType.SYMBOL;
    }

    public boolean isVariable() {
        return type != IdentifierType.SYMBOL;
    }

    @Override
    public String toString() {
        return "IdentifierDescriptor{" +
                "type=" + type +
                ", range=" + range +
                '}';
    }
}
