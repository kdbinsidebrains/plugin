package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public record TestDescriptor(@NotNull TestItem suite, @Nullable TestItem testCase, TestItem altItem) {
    public static final String SUITE = ".tst.desc";
    public static final String ALT = "alt";
    public static final String SHOULD = "should";
    public static final String HOLDS = "holds";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    public static final String SUITE_URI_SPEC = "qspec:suite://";
    public static final String TEST_URI_SPEC = "qspec:test://";

    private static final Set<String> POSSIBLE_PARENTS = Set.of(SUITE, ALT, SHOULD, HOLDS, BEFORE, AFTER);

    public static boolean hasTestCases(QFile file) {
        final List<QInvokeFunction> invokes = PsiTreeUtil.getChildrenOfTypeAsList(file, QInvokeFunction.class);
        if (invokes.isEmpty()) {
            return false;
        }

        for (QInvokeFunction invoke : invokes) {
            final QVarReference name = getFunctionName(invoke);
            if (name != null && SUITE.equals(name.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    public static TestItem findDescItem(QFile file, String name) {
        final List<QInvokeFunction> invokes = PsiTreeUtil.getChildrenOfTypeAsList(file, QInvokeFunction.class);
        if (invokes.isEmpty()) {
            return null;
        }

        final String text = '"' + name + '"';
        for (QInvokeFunction invoke : invokes) {
            final QVarReference ref = searchNameReference(invoke, text, s -> s.equals(SUITE));
            if (ref != null) {
                return TestItem.of(ref, invoke);
            }
        }
        return null;
    }

    public static TestItem findShouldItem(TestItem descItem, String name) {
        if (!(descItem.getInvoke().getExpression() instanceof QLambdaExpr lambda)) {
            return null;
        }

        final QExpressions expressions = lambda.getExpressions();
        if (expressions == null) {
            return null;
        }

        final String text = '"' + name + '"';
        final List<QExpression> expressionList = expressions.getExpressionList();
        for (QExpression exp : expressionList) {
            if (!(exp instanceof QInvokeFunction invoke)) {
                continue;
            }
            final QVarReference ref = searchNameReference(invoke, text, TestDescriptor::shouldName);
            if (ref != null) {
                return TestItem.of(ref, invoke);
            }
        }
        return null;
    }

    public static List<TestItem> findAllTestItems(@NotNull TestItem desc) {
        if (!(desc.getInvoke().getExpression() instanceof QLambdaExpr lambda) || lambda.getExpressions() == null) {
            return List.of();
        }

        final List<QExpression> expressions = lambda.getExpressions().getExpressionList();
        if (expressions.isEmpty()) {
            return List.of();
        }

        final List<TestItem> items = new ArrayList<>(expressions.size());
        for (QExpression expression : expressions) {
            if (expression instanceof QInvokeFunction invoke) {
                final QVarReference ref = getFunctionName(invoke);
                if (ref == null) {
                    continue;
                }

                final String name = ref.getQualifiedName();
                if (name.equals(SHOULD) || name.equals(HOLDS) || name.equals(BEFORE) || name.equals(AFTER)) {
                    items.add(TestItem.of(ref, invoke));
                }
            }
        }
        return items;
    }

    /**
     * Return testCase, before, after, alt or .tst.suite item, depend on what wa found first.
     * <p>
     * The method doesn't check that the element is inside a test so {@link TestDescriptor#of(PsiElement)} testCase be used before or after.
     *
     * @param element original element
     */
    public static TestItem getDirectParent(PsiElement element) {
        QInvokeFunction invoke = PsiTreeUtil.getParentOfType(element, QInvokeFunction.class);
        while (invoke != null) {
            final QVarReference ref = getFunctionName(invoke);
            if (ref != null) {
                final String name = ref.getQualifiedName();
                if (POSSIBLE_PARENTS.contains(name)) {
                    return TestItem.of(ref, invoke);
                }
            }
            invoke = PsiTreeUtil.getParentOfType(invoke, QInvokeFunction.class);
        }
        return null;
    }

    public static String testPath(VirtualFile file) {
        return testPath(file.toNioPath());
    }

    public static String testPath(Path path) {
        return FilenameUtils.normalize(path.toAbsolutePath().toString(), true);
    }

    public static boolean isRunnableItem(String name) {
        return SUITE.equals(name) || shouldName(name);
    }

    public static boolean isRunnableItem(QVarReference ref) {
        return isRunnableItem(ref.getQualifiedName());
    }

    public static TestDescriptor of(@NotNull PsiElement element) {
        TestItem should = null;
        TestItem altBlock = null;
        for (PsiElement el = element; el != null; el = el.getParent()) {
            if (!(el instanceof QInvokeFunction invoke)) {
                continue;
            }

            final QVarReference ref = getFunctionName(invoke);
            if (ref == null) {
                continue;
            }

            final String elementName = ref.getQualifiedName();
            if (shouldName(elementName)) {
                should = TestItem.of(ref, invoke);
            } else if (ALT.equals(elementName)) {
                altBlock = should != null ? TestItem.of(ref, invoke) : null;
            } else if (SUITE.equals(elementName) && invoke.getParent() instanceof QFile) { // be sure suite in root namespace
                final TestItem desc = TestItem.of(ref, invoke);
                return new TestDescriptor(desc, should, altBlock);
            }
        }
        return null;
    }

    public static boolean shouldName(String s) {
        return s.equals(SHOULD) || s.equals(HOLDS);
    }

    public static QVarReference getFunctionName(QInvokeFunction invoke) {
        final QCustomFunction customFunction = invoke.getCustomFunction();
        if (customFunction != null && customFunction.getExpression() instanceof QVarReference ref) {
            return ref;
        }
        return null;
    }

    private static @Nullable QVarReference searchNameReference(QInvokeFunction invoke, String caption, Predicate<String> functionName) {
        final QCustomFunction cf = invoke.getCustomFunction();
        if (cf == null) {
            return null;
        }

        final QExpression expression = cf.getExpression();
        if (!(expression instanceof QVarReference ref)) {
            return null;
        }

        if (!functionName.test(ref.getQualifiedName())) {
            return null;
        }

        final List<QArguments> arguments = invoke.getArgumentsList();
        if (arguments.isEmpty()) {
            return null;
        }

        final List<QExpression> expressions = arguments.get(0).getExpressions();
        if (expressions.isEmpty()) {
            return null;
        }

        final QExpression qExpression = expressions.get(0);
        if (!(qExpression instanceof QLiteralExpr lit)) {
            return null;
        }

        if (caption.equals(lit.getText())) {
            return ref;
        }
        return null;
    }

    public @NotNull String createUrl() {
        final String script = testPath(suite.getInvoke().getContainingFile().getVirtualFile());
        if (testCase == null) {
            return SUITE_URI_SPEC + script + "?[" + suite.getCaption() + "]";
        }
        return testCase + script + "?[" + suite.getCaption() + "]/[" + testCase.getCaption() + "]";
    }
}