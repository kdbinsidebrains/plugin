package org.kdb.inside.brains.lang.usage;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.ElementScope;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;

/**
 * TODO: in progress. Symbol is not supported yet.
 */
public final class QUsageTypeProvider implements UsageTypeProvider {
    public static final UsageType SYMBOL_REFERENCE = new UsageType(() -> "Symbol references");
    public static final UsageType VARIABLE_REFERENCE = new UsageType(() -> "Variable references");
    public static final UsageType PARAMETER = new UsageType(() -> "Lambda parameter");
    public static final UsageType TABLE_COLUMN = new UsageType(() -> "Table column");
    public static final UsageType QUERY_COLUMN = new UsageType(() -> "Query column");
    public static final UsageType LOCAL_ASSIGNMENT = new UsageType(() -> "Local assignment");
    public static final UsageType GLOBAL_ASSIGNMENT = new UsageType(() -> "Global assignment");
    public static final UsageType UNKNOWN_ASSIGNMENT = new UsageType(() -> "Unknown assignment");

    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        if (element instanceof QVariable) {
            final QVariable variable = (QVariable) element;

            final ElementScope type = QPsiUtil.getAssignmentType(variable);
            if (type == null) {
                return VARIABLE_REFERENCE;
            }
            switch (type) {
                case LAMBDA:
                    return LOCAL_ASSIGNMENT;
                case FILE:
                    return GLOBAL_ASSIGNMENT;
                case PARAMETERS:
                    return PARAMETER;
                case TABLE:
                    return TABLE_COLUMN;
                case QUERY:
                    return QUERY_COLUMN;
                default:
                    return UNKNOWN_ASSIGNMENT;
            }
        } else if (element instanceof QSymbol) {
            return SYMBOL_REFERENCE;
        }
        return null;
    }
}