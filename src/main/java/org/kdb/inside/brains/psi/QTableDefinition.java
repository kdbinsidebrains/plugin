package org.kdb.inside.brains.psi;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

public interface QTableDefinition extends QExpression {
    @Nullable
    default QTableColumns getKeys() {
        return PsiTreeUtil.getChildOfType(this, QTableKeys.class);
    }

    @Nullable
    default QTableColumns getValues() {
        return PsiTreeUtil.getChildOfType(this, QTableValues.class);
    }
}
