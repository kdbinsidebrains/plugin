package org.kdb.inside.brains.lang.annotation;

import com.intellij.codeInspection.util.IntentionFamilyName;
import org.jetbrains.annotations.NotNull;

public class WriteIntentionAction extends BaseIntentionAction {
    public WriteIntentionAction(@IntentionFamilyName @NotNull String text, @NotNull IntentionInvoke invoke) {
        super(text, invoke);
    }

    public WriteIntentionAction(@IntentionFamilyName @NotNull String familyName, @IntentionFamilyName @NotNull String text, @NotNull IntentionInvoke invoke) {
        super(familyName, text, invoke);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}