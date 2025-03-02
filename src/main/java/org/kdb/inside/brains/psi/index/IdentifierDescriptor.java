package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.util.TextRange;

import java.util.List;

public record IdentifierDescriptor(IdentifierType type, TextRange range, List<String> params) {
    public IdentifierDescriptor(IdentifierType type, TextRange range) {
        this(type, range, List.of());
    }

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
                ", params=" + params +
                '}';
    }
}
