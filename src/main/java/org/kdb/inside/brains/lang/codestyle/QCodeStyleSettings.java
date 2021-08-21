package org.kdb.inside.brains.lang.codestyle;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class QCodeStyleSettings extends CustomCodeStyleSettings {
    public boolean SPACE_BEFORE_BRACE_CLOSE = true;
    public boolean SPACE_AFTER_LAMBDA_PARAMETERS = true;

    protected QCodeStyleSettings(CodeStyleSettings container) {
        super("QCodeStyleSettings", container);
    }
}
