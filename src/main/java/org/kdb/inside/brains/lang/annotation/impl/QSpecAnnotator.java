package org.kdb.inside.brains.lang.annotation.impl;

import com.esotericsoftware.kryo.kryo5.util.Null;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.IntentionInvoke;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.lang.annotation.WriteIntentionAction;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;
import org.kdb.inside.brains.psi.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QSpecAnnotator extends QElementAnnotator<QInvokeFunction> {

    public static final String FAMILY_NAME = "QSpec testing framework";

    public QSpecAnnotator() {
        super(QInvokeFunction.class);
    }

    @Override
    protected void annotate(@NotNull QInvokeFunction invoke, @NotNull AnnotationHolder holder) {
        final QVarReference ref = TestDescriptor.getFunctionName(invoke);
        if (ref == null || !TestDescriptor.SUITE.equals(ref.getQualifiedName())) {
            return;
        }

        final TestItem desc = TestItem.of(ref, invoke);
        validateCaseItem(desc, holder, new HashSet<>());
        validateItems(desc, holder, new HashSet<>());
    }

    private void validateItems(@NotNull TestItem desc, @NotNull AnnotationHolder holder, Set<String> testNames) {
        TestItem before = null;
        TestItem after = null;

        final List<TestItem> testItems = TestDescriptor.getTestItems(desc);
        for (TestItem item : testItems) {
            switch (item.getName()) {
                case TestDescriptor.SHOULD, TestDescriptor.HOLDS -> validateCaseItem(item, holder, testNames);
                case TestDescriptor.BEFORE -> before = validateInitItem(item, before, holder);
                case TestDescriptor.AFTER -> after = validateInitItem(item, after, holder);
                case TestDescriptor.ALT -> validateItems(item, holder, testNames);
            }
        }
    }


    private TestItem validateInitItem(@NotNull TestItem item, @Null TestItem exist, @NotNull AnnotationHolder holder) {
        final QInvokeFunction invoke = item.getInvoke();
        final List<QArguments> argumentsList = invoke.getArgumentsList();
        if (!argumentsList.isEmpty()) {
            for (QArguments a : argumentsList) {
                holder.newAnnotation(HighlightSeverity.ERROR, "@" + item.getName() + " can't have arguments").range(a).withFix(new RemoveExcessiveArguments(a)).create();
            }
        }

        if (exist == null) {
            return item;
        }
        holder.newAnnotation(HighlightSeverity.ERROR, "@" + item.getName() + " already defined int he scope").range(item.getNameElement()).withFix(new MergeInitItem(item, exist)).create();
        return exist;
    }

    private void validateCaseItem(@NotNull TestItem item, @NotNull AnnotationHolder holder, @NotNull Set<String> testNames) {
        try {
            final QInvokeFunction invoke = item.getInvoke();
            final int argumentsMustCount = TestDescriptor.HOLDS.equals(item.getName()) ? 2 : 1;

            int expectedArgumentsCount = argumentsMustCount;
            final QVarReference nameElement = item.getNameElement();
            final List<QArguments> argumentsList = invoke.getArgumentsList();
            final int argumentsSize = argumentsList.size();
            if (argumentsSize == 0) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Test item doesn't have a name").range(nameElement).withFix(new NoNameIntentionAction(nameElement)).create();
            } else if (argumentsSize > expectedArgumentsCount) {
                for (int i = expectedArgumentsCount; i < argumentsSize; i++) {
                    final QArguments arguments = argumentsList.get(i);
                    holder.newAnnotation(HighlightSeverity.ERROR, "Test item can take only one name parameter").range(arguments).withFix(new RemoveExcessiveArguments(arguments)).create();
                }
            } else {
                expectedArgumentsCount -= argumentsSize - 1;

                final QArguments arguments = argumentsList.get(0);
                final List<QExpression> expressions = arguments.getExpressions();
                final int expressionsSize = expressions.size();
                if (expressionsSize == 0) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Test item doesn't have a name").range(nameElement).withFix(new NoNameIntentionAction(arguments)).create();
                } else if (expressionsSize > expectedArgumentsCount) {
                    for (int i = expectedArgumentsCount; i < expressionsSize; i++) {
                        final QExpression expression = expressions.get(i);
                        holder.newAnnotation(HighlightSeverity.ERROR, "Test item can take only one name parameter").range(expression).withFix(new RemoveExcessiveArguments(expression)).create();
                    }
                } else if (expressionsSize < expectedArgumentsCount) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Test item takes " + argumentsMustCount + " parameters").range(arguments).withFix(new InsertHoldsArguments(arguments)).create();
                } else {
                    final QExpression expression = expressions.get(0);
                    if (expression instanceof QLiteralExpr lit) {
                        final String testName = lit.getText();
                        if (lit.getFirstChild() instanceof LeafPsiElement leaf && leaf.getElementType() != QTypes.STRING && leaf.getElementType() != QTypes.CHAR) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Test name must be string").range(expression).withFix(new ArgumentToString(expression)).create();
                        } else if ("\"\"".equals(testName)) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Test name can't be empty").range(expression).withFix(new GenerateTestName(lit)).create();
                        } else if (!testNames.add(testName)) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Test with the same name already defined").range(lit).withFix(new RenameTest(lit)).create();
                        }
                    } else {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Test name must be a string").range(expression).withFix(new ArgumentToString(expression)).create();
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    private abstract static class QSpecIntentionAction extends WriteIntentionAction {
        public QSpecIntentionAction(String text, IntentionInvoke invoke) {
            super(FAMILY_NAME, text, invoke);
        }

        @Override
        public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            return IntentionPreviewInfo.EMPTY;
        }
    }

    private static class GenerateTestName extends QSpecIntentionAction {
        public GenerateTestName(PsiElement element) {
            super("Generate random test name", (p, e, f) -> {
                final Document document = e.getDocument();
                document.insertString(element.getTextOffset() + 1, UUID.randomUUID().toString());
            });
        }
    }

    private static class ArgumentToString extends QSpecIntentionAction {
        public ArgumentToString(PsiElement element) {
            super("Convert value to string", (p, e, f) -> {
                final TextRange range = element.getTextRange();
                final Document document = e.getDocument();
                document.replaceString(range.getStartOffset(), range.getEndOffset(), '"' + document.getText(range).replace("\"", "\\\"") + '"');

            });
        }
    }

    private static class NoNameIntentionAction extends QSpecIntentionAction {
        public NoNameIntentionAction(PsiElement element) {
            super("Insert test name", (p, e, f) -> {
                if (element instanceof QArguments) {
                    final TextRange textRange = element.getTextRange();
                    e.getDocument().insertString(textRange.getStartOffset() + 1, "\"\"");
                    e.getCaretModel().moveToOffset(textRange.getStartOffset() + 2);
                } else {
                    final TextRange textRange = element.getTextRange();
                    e.getDocument().insertString(textRange.getEndOffset(), "[\"\"]");
                    e.getCaretModel().moveToOffset(textRange.getEndOffset() + 2);
                }
            });
        }
    }

    private static class RemoveExcessiveArguments extends QSpecIntentionAction {
        public RemoveExcessiveArguments(PsiElement element) {
            super("Remove excessive arguments", (p, e, f) -> {
                PsiElement sibling = element.getPrevSibling();
                while (QPsiUtil.isWhitespace(sibling) || QPsiUtil.isSemicolon(sibling)) {
                    sibling = sibling.getPrevSibling();
                }

                if (sibling != null) {
                    element.getParent().deleteChildRange(sibling.getNextSibling(), element);
                } else {
                    element.delete();
                }
            });
        }
    }

    private static class InsertHoldsArguments extends QSpecIntentionAction {
        public InsertHoldsArguments(QArguments element) {
            super("Insert empty dictionary", (p, e, f) -> {
                final TextRange textRange = element.getTextRange();
                e.getDocument().insertString(textRange.getEndOffset() - 1, "; ()!()");
                e.getCaretModel().moveToOffset(textRange.getEndOffset() + 2);
                e.getSelectionModel().setSelection(textRange.getEndOffset() + 1, textRange.getEndOffset() + 6);
            });
        }
    }

    private static class MergeInitItem extends QSpecIntentionAction {
        public MergeInitItem(@NotNull TestItem item, @NotNull TestItem exist) {
            super("Merge and remove this one", (p, e, f) -> {
                final QExpressions expressions = item.getExpressions();
                if (expressions != null) {
                    final QExpressions existExpressions = exist.getExpressions();
                    if (existExpressions == null) {
                        final QLambdaExpr lambda = exist.getLambda();
                        if (lambda != null) {
                            final PsiElement lastChild = lambda.getLastChild();
                            lambda.addBefore(expressions, lastChild);
                            lambda.addBefore(QPsiUtil.createWhitespace(p, "\n"), lastChild);
                        }
                    } else if (expressions.getChildren().length > 0) {
                        existExpressions.add(QPsiUtil.createWhitespace(p, "\n\n"));
                        final @NotNull PsiElement[] children = expressions.getChildren();
                        for (@NotNull PsiElement child : children) {
                            existExpressions.add(child);
                        }
                    }
                }

                final QInvokeFunction invoke = item.getInvoke();
                PsiElement sibling = invoke.getNextSibling();
                while (QPsiUtil.isWhitespace(sibling) || QPsiUtil.isSemicolon(sibling)) {
                    sibling = sibling.getNextSibling();
                }

                if (sibling != null) {
                    invoke.getParent().deleteChildRange(invoke, sibling.getPrevSibling());
                } else {
                    invoke.delete();
                }
            });
        }
    }

    private static class RenameTest extends QSpecIntentionAction {
        public RenameTest(QLiteralExpr lit) {
            super("Rename the test case", (project, editor, file) -> {
                editor.getSelectionModel().setSelection(lit.getTextOffset() + 1, lit.getTextOffset() + lit.getTextLength() - 1);
            });
        }
    }
}
