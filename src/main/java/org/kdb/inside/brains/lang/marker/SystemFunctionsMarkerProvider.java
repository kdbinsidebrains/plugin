package org.kdb.inside.brains.lang.marker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QVarDeclaration;

import java.util.Collection;

public class SystemFunctionsMarkerProvider extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof QAssignmentExpr)) {
            return;
        }

        // TODO: check any function override
        final QAssignmentExpr assignment = (QAssignmentExpr) element;
        final QVarDeclaration variable = assignment.getVarDeclaration();
        if (variable == null) {
            return;
        }

        if (QLanguage.isSystemFunction(variable.getQualifiedName())) {
            final NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                            .setTargets(variable)
                            .setTooltipText("System function is overridden");
            result.add(builder.createLineMarkerInfo(variable.getFirstChild()));
        }
    }
}
