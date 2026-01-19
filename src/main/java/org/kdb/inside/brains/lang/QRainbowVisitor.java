package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.editor.EditorOptions;

import java.util.function.Predicate;

public class QRainbowVisitor extends RainbowVisitor {
    private final EditorOptions editorOptions;

    public QRainbowVisitor() {
        editorOptions = KdbSettingsService.getInstance().getEditorOptions();
    }

    @Override
    public boolean suitableForFile(@NotNull PsiFile psiFile) {
        return QFileType.is(psiFile);
    }

    @Override
    public void visit(@NotNull PsiElement psiElement) {
        if (psiElement instanceof @NotNull QVariable var) {
            if (editorOptions.isRainbowVariables()) {
                createVariableInfo(var);
            }
        } else {
            final IElementType type = psiElement.getNode().getElementType();
            if (type == QTypes.BRACE_OPEN || type == QTypes.BRACE_CLOSE) {
                if (editorOptions.isRainbowBrace()) {
                    createBraceInfo(psiElement);
                }
            } else if (type == QTypes.PAREN_OPEN || type == QTypes.PAREN_CLOSE) {
                if (editorOptions.isRainbowParen()) {
                    createParenInfo(psiElement);
                }
            } else if (type == QTypes.BRACKET_OPEN || type == QTypes.BRACKET_CLOSE) {
                if (editorOptions.isRainbowBracket()) {
                    createBracketInfo(psiElement);
                }
            }
        }
    }

    private void createBraceInfo(@NotNull PsiElement psiElement) {
        createBracketsInfo(psiElement, "q_rainbow_brace", QSyntaxHighlighter.BRACES, e -> e instanceof QLambdaExpr);
    }

    private void createParenInfo(@NotNull PsiElement psiElement) {
        createBracketsInfo(psiElement, "q_rainbow_paren", QSyntaxHighlighter.PARENTHESES, e -> e instanceof QParenthesesExpr);
    }

    private void createBracketInfo(@NotNull PsiElement psiElement) {
        createBracketsInfo(psiElement, "q_rainbow_bracket", QSyntaxHighlighter.BRACKETS, e -> e instanceof QConditionExpr || e instanceof QArguments || e instanceof QControlExpr || e instanceof QParameters || e instanceof QGroupingExpr);
    }

    private void createVariableInfo(@NotNull QVariable var) {
        final QLambda lambda = var.getContext(QLambda.class);
        if (lambda == null) {
            return;
        }

        if (lambda.isImplicitDeclaration(var)) {
            createVariableInfo(lambda, var);
        } else if (var instanceof QVarDeclaration d) {
            createDeclarationInfo(lambda, d, var);
        } else {
            final PsiReference[] references = var.getReferences();
            for (PsiReference reference : references) {
                final PsiElement resolve = reference.resolve();
                if (resolve instanceof QVarDeclaration d) {
                    createDeclarationInfo(lambda, d, var);
                    break;
                }
            }
        }
    }

    private void createDeclarationInfo(@NotNull QLambda lambda, @NotNull QVarDeclaration dec, @NotNull QVariable var) {
        if (QPsiUtil.isGlobalDeclaration(dec)) {
            return;
        }

        final ElementScope scope = dec.getVariableContext().getScope();
        // only root variables, no tables, dicts and so on
        if (scope == ElementScope.PARAMETERS || scope == ElementScope.LAMBDA) {
            createVariableInfo(lambda, var);
        }
    }

    private void createVariableInfo(@NotNull QLambda lambda, @NotNull QVariable var) {
        addInfo(getInfo(lambda, var, var.getName(), QSyntaxHighlighter.VARIABLE));
    }

    private void createBracketsInfo(PsiElement psiElement, String name, TextAttributesKey key, Predicate<PsiElement> predicate) {
        final DepthInfo info = calculateDepth(psiElement, predicate);
        if (info.context == null || info.depth < 0) {
            return;
        }
        addInfo(getInfo(info.context, psiElement, name + info.depth, key));
    }

    private DepthInfo calculateDepth(@NotNull PsiElement psiElement, Predicate<PsiElement> predicate) {
        int depth = 0;
        PsiElement context = null;
        PsiElement parent = psiElement.getParent();
        while (parent != null) {
            if (predicate.test(parent)) {
                context = parent;
                depth++;
            }
            parent = parent.getParent();
        }
        return new DepthInfo(context, depth - 1);
    }

    @Override
    public @NotNull HighlightVisitor clone() {
        return new QRainbowVisitor();
    }

    private record DepthInfo(PsiElement context, int depth) {
    }
}