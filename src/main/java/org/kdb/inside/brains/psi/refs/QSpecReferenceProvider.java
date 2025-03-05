package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;
import org.kdb.inside.brains.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class QSpecReferenceProvider extends QBaseReferenceProvider<QVariable> {
    private static final String MOCK_FUNCTION = "mock";

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
        if (TestDescriptor.SUITE.equals(qualifiedName)) {
            return PsiReference.EMPTY_ARRAY;
        }

        final boolean assertNs = ASSERTS.contains(qualifiedName);
        final boolean keywordNs = KEYWORDS.contains(qualifiedName);
        if (!assertNs && !keywordNs) {
            return maybeMock(var);
        }

        // not a QSpec test
        final TestDescriptor desc = TestDescriptor.of(var);
        if (desc == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return KeywordReference.of(var, keywordNs ? ".tst." : ".tst.asserts.");
    }

    private PsiReference[] maybeMock(@NotNull QVariable var) {
        return new PsiReference[]{new MockReference(var)};
    }

    public static class MockReference extends QBaseReference<QVariable> {
        public MockReference(@NotNull QVariable element) {
            super(element);
        }

        @Override
        protected String getQualifiedName(QVariable element) {
            return element.getQualifiedName();
        }

        @Override
        protected ResolveResult[] resolveElement(QVariable element) {
            final TestDescriptor desc = TestDescriptor.of(element);
            if (desc == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final TestItem testCase = desc.testCase();
            if (testCase == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final String name = element.getQualifiedName();

            final List<QSymbol> symbols = new ArrayList<>();
            // test case
            collectMockDeclarations(testCase.getLambda(), name, symbols);
            // or local before
            final TestItem before = desc.getLocalBefore();
            if (before != null) {
                collectMockDeclarations(before.getLambda(), name, symbols);
            }
            // nothing else
            return symbols.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
        }

        private void collectMockDeclarations(@Nullable PsiElement el, String name, List<QSymbol> symbols) {
            if (el == null) {
                return;
            }

            final Collection<QVarReference> vars = PsiTreeUtil.findChildrenOfType(el, QVarReference.class);
            if (vars.isEmpty()) {
                return;
            }

            for (QVarReference ref : vars) {
                final QSymbol symbol = getMockDeclaration(ref);
                if (symbol != null && name.equals(symbol.getQualifiedName())) {
                    symbols.add(symbol);
                }
            }
        }

        private QSymbol getMockDeclaration(QVarReference ref) {
            if (!MOCK_FUNCTION.equals(ref.getQualifiedName())) {
                return null;
            }
            if (!(ref.getParent() instanceof QCustomFunction qf) || !(qf.getParent() instanceof QInvokeFunction mockInv)) {
                return null;
            }

            if (mockInv.getParent() instanceof QInvokeFunction inv) {
                final QCustomFunction cf = inv.getCustomFunction();
                if (cf != null && cf.getExpression() instanceof QLiteralExpr ex) {
                    return ex.getSymbol();
                }
            } else {
                final List<QArguments> argumentsList = mockInv.getArgumentsList();
                if (argumentsList.isEmpty()) {
                    return null;
                }

                final List<QExpression> expressions = argumentsList.get(0).getExpressions();
                if (expressions.isEmpty()) {
                    return null;
                }

                if (!(expressions.get(0) instanceof QLiteralExpr lit)) {
                    return null;
                }
                return lit.getSymbol();
            }
            return null;
        }
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
