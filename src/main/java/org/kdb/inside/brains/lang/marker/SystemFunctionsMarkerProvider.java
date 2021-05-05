package org.kdb.inside.brains.lang.marker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QAssignment;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Collection;

public class SystemFunctionsMarkerProvider extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof QAssignment)) {
            return;
        }

        // TODO: check any function override
        final QAssignment assignment = (QAssignment) element;
        final QVariable variable = assignment.getVariable();
        if (variable == null) {
            return;
        }

        if (QLanguage.isSystemFunction(variable.getQualifiedName())) {
            final NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                            .setTargets(variable)
                            .setTooltipText("System function is overriden");
            result.add(builder.createLineMarkerInfo(variable.getFirstChild()));
        }
    }
}
