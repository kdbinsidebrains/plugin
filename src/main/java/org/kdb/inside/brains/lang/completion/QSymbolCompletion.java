package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.CastType;
import org.kdb.inside.brains.psi.QTableColumn;

import java.util.stream.Stream;

public class QSymbolCompletion extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement symbol = parameters.getOriginalPosition();
        if (symbol == null) {
            return;
        }

        addColumnTypeCast(symbol, result);
    }

    private void addColumnTypeCast(PsiElement symbol, CompletionResultSet result) {
        PsiElement el = symbol;
        while (el != null && !(el instanceof QTableColumn)) {
            el = el.getParent();
        }
        if (el == null) {
            return;
        }

        final String begin = symbol.getText().substring(1);
        Stream.of(CastType.values()).filter(t -> t.name.startsWith(begin)).forEach(t -> {
            final String s = t.name + "$()";
            result.addElement(LookupElementBuilder.create(s).withPresentableText("`" + s).withTypeText(t.lowerCode));
        });
    }
}
