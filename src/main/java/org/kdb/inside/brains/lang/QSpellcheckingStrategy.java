package org.kdb.inside.brains.lang;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import com.intellij.spellchecker.tokenizer.TokenizerBase;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QVariable;

public class QSpellcheckingStrategy extends SpellcheckingStrategy {
    final TokenizerBase<PsiElement> VARIABLE_TOKENIZER = TokenizerBase.create(PlainTextSplitter.getInstance());

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Tokenizer<PsiElement> getTokenizer(PsiElement element) {
        if (element instanceof QVariable) {
            return VARIABLE_TOKENIZER;
        }
        return super.getTokenizer(element);
    }
}
