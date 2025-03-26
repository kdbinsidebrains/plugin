package org.kdb.inside.brains.lang.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.impl.*;
import org.kdb.inside.brains.psi.QPsiElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QLanguageAnnotator implements Annotator {
    private final List<QElementAnnotator<? extends QPsiElement>> raw = List.of(
            new QParametersAnnotator(),
            new QContextAnnotator(),
            new QTypeCastAnnotator(),
            new QImportAnnotator(),
            QFlipAnnotator.newDictAnnotator(),
            QFlipAnnotator.newTableAnnotator(),
            new QSpecAnnotator()
    );

    private final Map<Class<? extends PsiElement>, QElementAnnotator<?>> annotators = new HashMap<>();

    private static final QElementAnnotator<QPsiElement> NO = new QElementAnnotator<>(QPsiElement.class) {
        @Override
        protected void annotate(@NotNull QPsiElement element, @NotNull AnnotationHolder holder) {
        }
    };

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof QPsiElement)) {
            return;
        }

        final Class<? extends PsiElement> aClass = element.getClass();
        final QElementAnnotator<?> annotator = this.annotators.computeIfAbsent(aClass, type -> {
            for (QElementAnnotator<? extends QPsiElement> a : raw) {
                if (a.getType().isAssignableFrom(type)) {
                    return a;
                }
            }
            return NO;
        });

        if (annotator != NO) {
            annotator.annotate(element, holder);
        }
    }
}
