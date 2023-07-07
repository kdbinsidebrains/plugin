package org.kdb.inside.brains.view;

public interface FormatterOptions {
    int getFloatPrecision();

    boolean isWrapStrings();

    boolean isPrefixSymbols();

    boolean isEnlistArrays();

    default boolean isThousandsSeparator() {
        return false;
    }
}
