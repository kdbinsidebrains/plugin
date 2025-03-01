package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.psi.QVarReference;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Set;

public class QSpecReferenceProvider extends QBaseReferenceProvider<QVariable> {
    private static final Set<String> KEYWORDS = Set.of(
            "should",
            "holds",
            "before",
            "after",
            "alt",
            "fixture",
            "fixtureAs",
            "mock"
    );

    private static final Set<String> ASSERTS = Set.of(
            "must",
            "musteq",
            "mustmatch",
            "mustnmatch",
            "mustne",
            "mustlt",
            "mustgt",
            "mustlike",
            "mustin",
            "mustnin",
            "mustwithin",
            "mustdelta",
            "mustthrow",
            "mustnotthrow"
    );

    protected QSpecReferenceProvider() {
        super(QVariable.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QVariable var, @NotNull ProcessingContext context) {
        if (!(var instanceof QVarReference)) {
            return PsiReference.EMPTY_ARRAY;
        }

        final String qualifiedName = var.getQualifiedName();
        final boolean assertNs = ASSERTS.contains(qualifiedName);
        final boolean keywordNs = KEYWORDS.contains(qualifiedName);
        if (!assertNs && !keywordNs) {
            return PsiReference.EMPTY_ARRAY;
        }

        // not a QSpec test
        final TestDescriptor desc = TestDescriptor.of(var);
        if (desc == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return KeywordReference.of(var, keywordNs ? ".tst." : ".tst.asserts.");
    }

    public static class KeywordReference extends QBaseReference<QVariable> {
        private final String namespace;

        public KeywordReference(@NotNull QVariable variable, String namespace) {
            super(variable);
            this.namespace = namespace;
        }

        public static PsiReference[] of(QVariable var, String namespace) {
            return new PsiReference[]{new KeywordReference(var, namespace)};
        }

        @Override
        protected String getQualifiedName(QVariable element) {
            return namespace + element.getQualifiedName();
        }
    }
}
