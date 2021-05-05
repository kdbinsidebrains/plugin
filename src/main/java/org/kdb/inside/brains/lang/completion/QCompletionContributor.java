package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import org.kdb.inside.brains.psi.QTypes;

public class QCompletionContributor extends CompletionContributor {
    public QCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(QTypes.VARIABLE_PATTERN), new QVariableCompletionContributor());
    }
}

