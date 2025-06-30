package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface QLambda extends QPsiElement {
    String getVariables();

    String getParametersInfo();

    @Nullable
    QParameters getParameters();

    @Nullable
    QExpressions getExpressions();

//    boolean isImplicitVariable(String name);

    boolean isImplicitDeclaration(@NotNull QVariable variable);


    @Nullable
    QVarDeclaration getLocalDeclaration(@NotNull QVariable variable);
}
