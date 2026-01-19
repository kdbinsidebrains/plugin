package org.kdb.inside.brains.lang.highlighting;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.editor.EditorOptions;

import java.util.List;

public class QVectorHighlighterFactory extends HighlightUsagesHandlerFactoryBase {
    private final EditorOptions editorOptions;

    public QVectorHighlighterFactory() {
        editorOptions = KdbSettingsService.getInstance().getEditorOptions();
    }

    @Override
    public @Nullable HighlightUsagesHandlerBase<PsiElement> createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement) {
        if (!editorOptions.isHighlightVector()) {
            return null;
        }

        PsiElement el = psiElement.getParent();
        while (el != null && !isDictionary(el)) {
            el = el.getParent();
        }

        if (!(el instanceof QInvokeExpr invoke)) {
            return null;
        }
        return new QDictItemsUsageHighlight(editor, psiFile, psiElement, invoke);
    }

    protected boolean isDictionary(PsiElement el) {
        if (!(el instanceof QInvokeExpr invoke)) {
            return false;
        }
        final @NotNull PsiElement[] children = invoke.getChildren();
        if (children.length != 3) {
            return false;
        }
        if (!QPsiUtil.isOperator(children[1], "!")) {
            return false;
        }
        return isDictionarySide(children[0]) && isDictionarySide(children[2]);
    }

    protected boolean isDictionarySide(PsiElement el) {
        if (el instanceof QParenthesesExpr) {
            return true;
        }

        if (el instanceof QCustomFunction qf) {
            if (qf.getExpression() instanceof QLiteralExpr expr) {
                return isDictionarySide(expr);
            }
        }

        if (el instanceof QLiteralExpr expr) {
            final PsiElement firstChild = expr.getFirstChild();
            return firstChild instanceof QSymbols || firstChild instanceof QVector;
        }
        return false;
    }

    public static final class QDictItemsUsageHighlight extends HighlightUsagesHandlerBase<PsiElement> {
        @NotNull
        private final PsiElement el;
        @NotNull
        private final QInvokeExpr invoke;

        public QDictItemsUsageHighlight(@NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull PsiElement el, @NotNull QInvokeExpr invoke) {
            super(editor, psiFile);
            this.el = el;
            this.invoke = invoke;
        }

        @NotNull
        @Override
        public List<PsiElement> getTargets() {
            return List.of(el);
        }

        @Override
        protected void selectTargets(@NotNull List<? extends PsiElement> targets, Consumer<? super List<? extends PsiElement>> selectionConsumer) {
            selectionConsumer.consume(targets);
        }

        @Override
        public void computeUsages(@NotNull List<? extends PsiElement> targets) {
            final PsiElement parent = el.getParent();
            final int index = findIndex(parent);
            if (index < 0) {
                return;
            }

            final @NotNull PsiElement[] children = invoke.getChildren();
            final PsiElement left = children[0];
            final PsiElement right = children[2];

            final Side side = findElementSide(parent, left, right);
            if (side == null) {
                return;
            }

            final Result result = findElementByIndex(index, side == Side.LEFT ? right : left);
            if (result == null) {
                return;
            }

//            if (parent instanceof QVector) { // special case - find the text range in the vector again
//                final Result vectorResult = findElementByIndex(index, side == Side.LEFT ? left : right);
//                if (vectorResult == null) {
//                    return;
//                }
//                addOccurrence(vectorResult.element, vectorResult.range);
//            } else {
//                addOccurrence(parent, parent.getTextRange());
//            }
            addOccurrence(parent, parent.getTextRange());
            addOccurrence(result.element, result.range);
        }

        void addOccurrence(@NotNull PsiElement element, TextRange range) {
            range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
            this.myReadUsages.add(range);
        }

        private int findIndex(PsiElement el) {
            if (el instanceof QSymbol s) {
                if (s.getParent() instanceof QSymbols symbols) {
                    return symbols.getSymbolList().indexOf(s);
                }
                if (s.getParent() instanceof QLiteralExpr) {
                    el = s.getParent();
                }
            }
//            Vector is not supported as IDEA caches the element, and we can't match it by a text region.
//            if (el instanceof QVector vector) {
//                return vector.getIndexForPosition(myEditor.getCaretModel().getOffset());
//            }

            if (el.getParent() instanceof QParenthesesExpr expr) {
                return expr.getExpressionList().indexOf(el);
            }
            return -1;
        }

        private Result findElementByIndex(int index, PsiElement psiElement) {
            if (psiElement == null) {
                return null;
            }
            if (psiElement instanceof QParenthesesExpr expr) {
                final List<QExpression> expressionList = expr.getExpressionList();
                if (expressionList.size() > index) {
                    return new Result(expressionList.get(index));
                }
                return null;
            }
            if (psiElement instanceof QCustomFunction qf) {
                return findElementByIndex(index, qf.getExpression());
            }
            if (psiElement instanceof QLiteralExpr ql) {
                final PsiElement firstChild = ql.getFirstChild();
                if (firstChild instanceof QSymbols symbols) {
                    final List<QSymbol> symbolList = symbols.getSymbolList();
                    if (symbolList.size() > index) {
                        return new Result(symbolList.get(index));
                    }
                    return null;
                }
                if (firstChild instanceof QVector vector) {
                    final TextRange range = vector.getRangeForIndex(index);
                    if (range != null) {
                        return new Result(vector, range);
                    }
                    return null;
                }
            }
            return null;
        }

        private Side findElementSide(PsiElement parent, PsiElement left, PsiElement right) {
            while (parent != null) {
                if (parent == left) {
                    return Side.LEFT;
                }
                if (parent == right) {
                    return Side.RIGHT;
                }
                parent = parent.getParent();
            }
            return null;
        }
    }

    private enum Side {
        LEFT, RIGHT
    }

    private record Result(PsiElement element, TextRange range) {
        private Result(PsiElement element) {
            this(element, element.getTextRange());
        }
    }
}
