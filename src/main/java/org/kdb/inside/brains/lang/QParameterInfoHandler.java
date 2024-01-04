package org.kdb.inside.brains.lang;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.util.*;

public class QParameterInfoHandler implements ParameterInfoHandler<QInvokeExpr, QParameterInfoHandler.QParameterInfo> {
    @Override
    public @Nullable QInvokeExpr findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        final QInvokeExpr invoke = findInvoke(context.getFile(), context.getOffset());
        if (invoke == null) {
            return null;
        }

        QVarReference ref = null;
        List<QLambdaExpr> lambdas = null;

        final QCustomFunction customFunction = invoke.getCustomFunction();
        if (customFunction == null) {
            return null;
        }
        final QExpression expression = customFunction.getExpression();
        if (expression instanceof QLambdaExpr) {
            lambdas = List.of((QLambdaExpr) expression);
        } else if (expression instanceof QVarReference) {
            ref = (QVarReference) expression;
            lambdas = findLambdasByName(ref);
        }

        if (lambdas == null || lambdas.isEmpty()) {
            return null;
        }

        final QParameterInfo[] params = distinctParameters(ref, lambdas);
        if (params.length == 0) {
            return null;
        }
        context.setItemsToShow(params);
        return invoke;
    }

    @Override
    public void showParameterInfo(@NotNull QInvokeExpr element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, context.getOffset(), this);
    }

    @Override
    public @Nullable QInvokeExpr findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        final QInvokeExpr invoke = findInvoke(context.getFile(), context.getOffset());
        if (invoke != null) {
            final PsiElement currentInvoke = context.getParameterOwner();
            if (currentInvoke == null || currentInvoke == invoke) {
                return invoke;
            }
        }
        return null;
    }

    @Override
    public void updateParameterInfo(@NotNull QInvokeExpr invoke, @NotNull UpdateParameterInfoContext context) {
        context.setParameterOwner(invoke);

        final int param = findCurrentParameter(invoke, context.getOffset());
        context.setCurrentParameter(param);
    }

    private int findCurrentParameter(@NotNull QInvokeExpr invoke, int offset) {
        final boolean[] busy = new boolean[8]; // 8 arguments, not more

        final List<QArguments> arguments = invoke.getArgumentsList();
        for (QArguments argument : arguments) {
            int index = findFreeIndex(busy, 0);

            PsiElement el = argument.getFirstChild();

            boolean expr = false;
            int pos = el.getTextOffset();
            while (el != null) {
                if (offset > pos && offset <= el.getTextOffset()) {
                    return index;
                }

                if (QPsiUtil.isSemicolon(el) || QPsiUtil.isLeafText(el, "]")) {
                    if (expr) {
                        busy[index] = true;
                        expr = false;
                    }
                    index = findFreeIndex(busy, index + 1);
                    pos = el.getTextOffset();
                } else if (el instanceof QExpression) {
                    expr = true;
                }
                el = PsiTreeUtil.skipWhitespacesAndCommentsForward(el);
            }
        }
        return -1;
    }

    private int findFreeIndex(boolean[] busy, int index) {
        while (index < busy.length && busy[index]) {
            index++;
        }
        return index;
    }

    @Override
    public void updateUI(QParameterInfo p, @NotNull ParameterInfoUIContext context) {
        int startOffset = -1;
        int endOffset = -1;
        final int index = context.getCurrentParameterIndex();

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.parameters.length; i++) {
            String parameter = p.parameters[i];
            if (i == index) {
                startOffset = b.length();
                endOffset = startOffset + parameter.length();
            }
            b.append(parameter);
            b.append(";");
        }
        if (!b.isEmpty()) {
            b.setLength(b.length() - 1);
        }
        context.setupUIComponentPresentation(b.toString(), startOffset, endOffset, false, false, true, context.getDefaultParameterColor());
    }

    private QParameterInfo[] distinctParameters(QVarReference ref, List<QLambdaExpr> lambdas) {
        final String name = ref == null ? null : ref.getQualifiedName();
        final LinkedHashSet<QParameterInfo> res = new LinkedHashSet<>();

        for (QLambdaExpr lambda : lambdas) {
            final QParameters parameters = lambda.getParameters();
            if (parameters == null) {
                res.add(new QParameterInfo(name, new String[]{"x"}));
                res.add(new QParameterInfo(name, new String[]{"x", "y"}));
                res.add(new QParameterInfo(name, new String[]{"x", "y", "z"}));
            } else {
                final List<QVarDeclaration> variables = parameters.getVariables();
                if (variables.isEmpty()) {
                    res.add(new QParameterInfo(name, new String[]{"no parameters"}));
                } else {
                    res.add(new QParameterInfo(name, variables.stream().map(QVariable::getName).toArray(String[]::new)));
                }
            }
        }
        return res.toArray(QParameterInfo[]::new);
    }

    private QInvokeExpr findInvoke(PsiFile file, int offset) {
        if (!(file instanceof QFile)) {
            return null;
        }
        return (QInvokeExpr) PsiTreeUtil.findFirstParent(file.findElementAt(offset), c -> c instanceof QInvokeExpr);
    }

    private List<QLambdaExpr> findLambdasByName(QVarReference ref) {
        final List<QLambdaExpr> lambdas = new ArrayList<>();

        final PsiReference[] references = ref.getReferences();
        for (PsiReference reference : references) {
            if (reference instanceof PsiPolyVariantReference) {
                final ResolveResult[] resolveResults = ((PsiPolyVariantReference) reference).multiResolve(false);
                for (ResolveResult resolveResult : resolveResults) {
                    if (resolveResult.isValidResult()) {
                        final QLambdaExpr qLambdaExpr = resolveLambdaExpr(resolveResult.getElement());
                        if (qLambdaExpr != null) {
                            lambdas.add(qLambdaExpr);
                        }
                    }
                }
            } else {
                final QLambdaExpr qLambdaExpr = resolveLambdaExpr(reference.resolve());
                if (qLambdaExpr != null) {
                    lambdas.add(qLambdaExpr);
                }
            }
        }
        return lambdas;
    }

    private QLambdaExpr resolveLambdaExpr(PsiElement resolve) {
        if (resolve == null) {
            return null;
        }

        final PsiElement parent = resolve.getParent();
        if (parent instanceof QAssignmentExpr assignment) {
            final QExpression expression = assignment.getExpression();
            if (expression instanceof QLambdaExpr) {
                return (QLambdaExpr) expression;
            }
        }
        return null;
    }

    protected static class QParameterInfo {
        private final String name;
        private final String[] parameters;

        public QParameterInfo(String name, String[] parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QParameterInfo that = (QParameterInfo) o;
            return Objects.equals(name, that.name) && Arrays.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name);
            result = 31 * result + Arrays.hashCode(parameters);
            return result;
        }
    }
}
