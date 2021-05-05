package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QSymbol;

public class QSymbolAnnotator extends QElementAnnotator<QSymbol> {
    public QSymbolAnnotator() {
        super(QSymbol.class);
    }

    @Override
    public void annotate(@NotNull QSymbol element, @NotNull AnnotationHolder holder) {
    }
}
