package org.kdb.inside.brains.lang.usage;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;

/**
 * TODO: in progress. Symbol is not supported yet.
 */
public final class QUsageTypeProvider implements UsageTypeProvider {
    public static final UsageType SYMBOL_REFERENCE = new UsageType(() -> "Symbols");
    public static final UsageType VARIABLE_REFERENCE = new UsageType(() -> "References");
    public static final UsageType VARIABLE_ASSIGNMENTS = new UsageType(() -> "Assignments");

    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        if (element instanceof QVariable) {
            final QVariable variable = (QVariable) element;
            return variable.isDeclaration() ? VARIABLE_ASSIGNMENTS : VARIABLE_REFERENCE;
        } else if (element instanceof QSymbol) {
            return SYMBOL_REFERENCE;
        }
        return null;
    }
}