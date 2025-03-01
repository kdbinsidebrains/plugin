package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;

import java.util.Set;

public class QSpecCompletion extends CompletionProvider<CompletionParameters> {
    private static final Set<String> STRUCTURE_ITEMS = Set.of(TestDescriptor.BEFORE, TestDescriptor.AFTER, TestDescriptor.SHOULD, TestDescriptor.HOLDS);

    private static final LookupElement ALT =
            LookupElementBuilder.create(TestDescriptor.ALT).withTypeText("QSpec helper", true);

    private static final Set<LookupElement> SPEC = Set.of(
            LookupElementBuilder.create(TestDescriptor.AFTER).withTypeText("QSpec helper", true),
            LookupElementBuilder.create(TestDescriptor.SHOULD).withTypeText("QSpec test case", true),
            LookupElementBuilder.create(TestDescriptor.HOLDS).withTypeText("QSpec test case", true),
            LookupElementBuilder.create(TestDescriptor.BEFORE).withTypeText("QSpec helper", true)
    );

    private static final Set<LookupElement> KEYWORDS = Set.of(
            // mocks
            LookupElementBuilder.create("mock").withTypeText("QSpec mock", true),
            LookupElementBuilder.create("fixture").withTypeText("QSpec fixture", true),
            LookupElementBuilder.create("fixtureAs").withTypeText("QSpec fixture", true),

            // keywords
            LookupElementBuilder.create("must").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("musteq").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustmatch").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustnmatch").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustne").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustlt").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustgt").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustlike").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustin").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustnin").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustwithin").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustdelta").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustthrow").withTypeText("QSpec assert", true),
            LookupElementBuilder.create("mustnotthrow").withTypeText("QSpec assert", true)
    );

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        PsiElement el = parameters.getOriginalPosition();
        if (el == null) {
            el = parameters.getPosition();
        }

        final TestDescriptor descriptor = TestDescriptor.of(el);
        if (descriptor == null) {
            return;
        }

        final TestItem inside = TestDescriptor.getDirectParent(el);
        if (inside == null) {
            return;
        }

        if (inside.is(TestDescriptor.SUITE)) {
            result.addElement(ALT);
            result.addAllElements(SPEC);
        } else if (inside.is(TestDescriptor.ALT)) {
            result.addAllElements(SPEC);
        } else if (inside.isAny(STRUCTURE_ITEMS)) {
            result.addAllElements(KEYWORDS);
        }
    }
}