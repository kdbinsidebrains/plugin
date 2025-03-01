package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TestItem {
    private final String name;
    private final QVarReference nameElement;
    private final QInvokeFunction invoke;

    private final String caption;
    private final QLiteralExpr captionElement;

    private TestItem(@NotNull QVarReference nameElement, @NotNull QInvokeFunction invoke) {
        this.name = nameElement.getQualifiedName();
        this.nameElement = nameElement;
        this.invoke = invoke;

        this.captionElement = getCaptionElement(invoke);
        this.caption = captionText(captionElement);
    }

    public static @NotNull TestItem of(@NotNull QVarReference reference, @NotNull QInvokeFunction invoke) {
        return new TestItem(reference, invoke);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public QVarReference getNameElement() {
        return nameElement;
    }

    @Nullable
    public String getCaption() {
        return caption;
    }

    @Nullable
    public QLiteralExpr getCaptionElement() {
        return captionElement;
    }

    @NotNull
    public QInvokeFunction getInvoke() {
        return invoke;
    }

    @NotNull
    public QLambdaExpr getLambda() {
        return (QLambdaExpr) Objects.requireNonNull(invoke.getExpression());
    }

    @Nullable
    public QExpressions getExpressions() {
        return getLambda().getExpressions();
    }

    public boolean is(String name) {
        return Objects.equals(this.name, name);
    }

    public boolean isAny(Set<String> names) {
        return names.contains(this.name);
    }

    public boolean hasNoCaption() {
        return StringUtil.isEmpty(caption);
    }

    private String captionText(@Nullable QLiteralExpr captionElement) {
        if (captionElement == null) {
            return null;
        }
        final String text = captionElement.getText();
        return text.substring(1, text.length() - 1);
    }

    private QLiteralExpr getCaptionElement(QInvokeFunction function) {
        if (function == null) {
            return null;
        }

        final List<QArguments> argumentsList = function.getArgumentsList();
        if (argumentsList.isEmpty()) {
            return null;
        }

        final List<QExpression> expressions = argumentsList.get(0).getExpressions();
        if (expressions.isEmpty()) {
            return null;
        }

        final QExpression qExpression = expressions.get(0);
        if (!(qExpression instanceof QLiteralExpr lit)) {
            return null;
        }

        if (lit.getFirstChild() instanceof LeafPsiElement leaf && (leaf.getElementType() == QTypes.STRING || leaf.getElementType() == QTypes.CHAR)) {
            return lit;
        }
        return null;
    }
}
