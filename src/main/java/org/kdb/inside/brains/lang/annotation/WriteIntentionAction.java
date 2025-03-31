package org.kdb.inside.brains.lang.annotation;

import com.intellij.codeInspection.util.IntentionFamilyName;
import org.jetbrains.annotations.NotNull;

public class WriteIntentionAction extends BaseIntentionAction {
    public WriteIntentionAction(@IntentionFamilyName @NotNull String text, @NotNull IntentionInvoke invoke) {
        super(text, invoke);
    }

    public WriteIntentionAction(@IntentionFamilyName @NotNull String text, @IntentionFamilyName @NotNull String familyName, @NotNull IntentionInvoke invoke) {
        super(text, familyName, invoke);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}