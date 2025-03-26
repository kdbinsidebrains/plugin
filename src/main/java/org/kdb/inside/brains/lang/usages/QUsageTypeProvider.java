package org.kdb.inside.brains.lang.usages;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

public final class QUsageTypeProvider implements UsageTypeProvider {
    public static final UsageType SYMBOL_REFERENCE = new UsageType(() -> "Symbol references");
    public static final UsageType VARIABLE_REFERENCE = new UsageType(() -> "Variable references");
    public static final UsageType PARAMETER = new UsageType(() -> "Lambda parameter");
    public static final UsageType DICT_FIELDS = new UsageType(() -> "Dict field");
    public static final UsageType TABLE_COLUMN = new UsageType(() -> "Table column");
    public static final UsageType QUERY_COLUMN = new UsageType(() -> "Query column");
    public static final UsageType LOCAL_ASSIGNMENT = new UsageType(() -> "Local assignment");
    public static final UsageType GLOBAL_ASSIGNMENT = new UsageType(() -> "Global assignment");
    public static final UsageType UNKNOWN_ASSIGNMENT = new UsageType(() -> "Unknown assignment");

    @Override
    public @Nullable UsageType getUsageType(@NotNull PsiElement element) {
        if (element instanceof QVarReference) {
            return VARIABLE_REFERENCE;
        }
        if (element instanceof QSymbol) {
            return SYMBOL_REFERENCE;
        }

        if (element instanceof QVarDeclaration declaration) {
            final ElementContext elementContext = QPsiUtil.getElementContext(declaration);
            final ElementScope scope = elementContext.getScope();
            return switch (scope) {
                case FILE -> GLOBAL_ASSIGNMENT;
                case DICT -> DICT_FIELDS;
                case TABLE -> TABLE_COLUMN;
                case QUERY -> QUERY_COLUMN;
                case LAMBDA -> LOCAL_ASSIGNMENT;
                case PARAMETERS -> PARAMETER;
                default -> UNKNOWN_ASSIGNMENT;
            };
        }
        return null;
    }
}