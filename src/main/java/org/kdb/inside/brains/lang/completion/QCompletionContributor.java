package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.patterns.PlatformPatterns;
import org.kdb.inside.brains.psi.QTypes;

public class QCompletionContributor extends CompletionContributor implements DumbAware {
    public QCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(QTypes.SYMBOL_PATTERN), new QSymbolCompletion());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(QTypes.VARIABLE_PATTERN), new QVariableCompletion());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(QTypes.VARIABLE_PATTERN), new QSpecCompletion());
    }
}