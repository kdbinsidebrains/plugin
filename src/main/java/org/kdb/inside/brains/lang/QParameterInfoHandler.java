package org.kdb.inside.brains.lang;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
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
        return null;

/*
    // TODO: COMMENTED

        final QVarReference ref;
        final QLambda rawLambda = invoke.getLambda();

        final List<QLambda> lambdas;
        if (rawLambda != null) {
            ref = null;
            lambdas = List.of(rawLambda);
        } else {
            ref = invoke.getVarReference();
            if (ref == null) {
                return null;
            }
            lambdas = findLambdasByName(ref);
        }

        if (lambdas.isEmpty()) {
            return null;
        }

        final QParameterInfo[] params = distinctParameters(ref, lambdas);
        if (params.length == 0) {
            return null;
        }
        context.setItemsToShow(params);
        return invoke;
*/
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

/*
TODO: Commented
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
*/
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
        if (b.length() != 0) {
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

        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }
        element = element.getParent();

        while (element != null) {
            if (element instanceof QInvokeExpr) {
                return (QInvokeExpr) element;
            }
            element = element.getParent();
        }
        return null;
    }

    private List<QLambdaExpr> findLambdasByName(QVarReference ref) {
        final List<QLambdaExpr> lambdas = new ArrayList<>();
        final PsiReference[] references = ref.getReferences();
        for (PsiReference reference : references) {
            final PsiElement resolve = reference.resolve();
            if (resolve == null) {
                continue;
            }

/*
TODO: COMMENTED
            final PsiElement parent = resolve.getParent();
            if (parent instanceof QVariableAssignment) {
                final QVariableAssignment assignment = (QVariableAssignment) parent;
                final QExpression expression = assignment.getExpression();
                if (expression != null && !expression.getLambdaList().isEmpty()) {
                    lambdas.addAll(expression.getLambdaList());
                }
            }
*/
        }
        return lambdas;
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



/*
    @Override
    public QArguments @NotNull [] getActualParameters(@NotNull QInvoke o) {
        return new QArguments[0];
    }

    @Override
    public @NotNull IElementType getActualParameterDelimiterType() {
        return QTypes.SEMICOLON;
    }

    @Override
    public @NotNull IElementType getActualParametersRBraceType() {
        return QTypes.BRACKET_CLOSE;
    }

    @Override
    public @NotNull Set<Class<?>> getArgumentListAllowedParentClasses() {
        return Set.of();
    }

    @Override
    public @NotNull Set<? extends Class<?>> getArgListStopSearchClasses() {
        return Set.of();
    }

    @Override
    public boolean isWhitespaceSensitive() {
        return false;
    }

    @Override
    public void showParameterInfo(@NotNull QInvoke element, @NotNull CreateParameterInfoContext context) {
    }

    @Override
    public @NotNull Class<QInvoke> getArgumentListClass() {
        return QInvoke.class;
    }

    @Override
    public @Nullable QInvoke findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        return null;
    }

    @Override
    public @Nullable QInvoke findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        return null;
    }

    @Override
    public void updateParameterInfo(@NotNull QInvoke qInvoke, @NotNull UpdateParameterInfoContext context) {

    }

    @Override
    public void updateUI(Object p, @NotNull ParameterInfoUIContext context) {

    }
*/
}
