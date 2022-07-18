package org.kdb.inside.brains.lang.usages;

import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewLongNameLocation;
import com.intellij.usageView.UsageViewShortNameLocation;
import org.jetbrains.annotations.NotNull;

public class QUsageViewDescriptionProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (location instanceof UsageViewShortNameLocation) {


        } else if (location instanceof UsageViewLongNameLocation) {

        }
/*
        if (element instanceof QVariable) {
            QVariable qVariable = (QVariable) element;

        }

        return "getElementDescription: " + element.getClass().getSimpleName();*/
        return null;
    }
}
