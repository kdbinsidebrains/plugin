package org.kdb.inside.brains.psi;

import com.intellij.openapi.util.TextRange;

public interface QVector extends QPsiElement {
    TextRange getRangeForIndex(int index);

    int getIndexForPosition(int globalPosition);
}