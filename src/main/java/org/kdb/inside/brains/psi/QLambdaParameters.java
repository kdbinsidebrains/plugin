package org.kdb.inside.brains.psi;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface QLambdaParameters extends QPsiElement {
    @NotNull
    List<QParameter> getParameters();

    default List<QVarDeclaration> getVariables() {
        final List<QVarDeclaration> res = new ArrayList<>();
        PsiTreeUtil.processElements(this, QVarDeclaration.class, res::add);
        return res;
    }

    @NotNull
    default String getParametersInfo() {
        return "[" + getParameters().stream().map(QParameter::getParameterInfo).collect(Collectors.joining(";")) + "]";
    }
}
