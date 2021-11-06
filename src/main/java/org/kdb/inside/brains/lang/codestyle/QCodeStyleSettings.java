package org.kdb.inside.brains.lang.codestyle;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import org.intellij.lang.annotations.MagicConstant;

public class QCodeStyleSettings extends CustomCodeStyleSettings {
    // Lambda
    public boolean LAMBDA_SPACE_AFTER_PARAMETERS = true;
    public boolean LAMBDA_SPACE_BEFORE_BRACE_CLOSE = true;

    // Controls
    @MagicConstant(flags = {
            CommonCodeStyleSettings.DO_NOT_WRAP,
            CommonCodeStyleSettings.WRAP_ALWAYS,
            CommonCodeStyleSettings.WRAP_AS_NEEDED,
            CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM
    })
    public int CONTROL_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONTROL_WRAP_ALIGN = true;
    public boolean CONTROL_OBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_CBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_SPACE_AFTER_OPERATOR = false;
    public boolean CONTROL_SPACE_WITHIN_BRACES = false;
    public boolean CONTROL_SPACE_AFTER_SEMICOLON = true;
    public boolean CONTROL_SPACE_BEFORE_SEMICOLON = false;

    // Conditions
    @MagicConstant(flags = {
            CommonCodeStyleSettings.DO_NOT_WRAP,
            CommonCodeStyleSettings.WRAP_ALWAYS,
            CommonCodeStyleSettings.WRAP_AS_NEEDED,
            CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM
    })
    public int CONDITION_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONDITION_WRAP_ALIGN = true;
    public boolean CONDITION_OBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_CBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_SPACE_AFTER_OPERATOR = false;
    public boolean CONDITION_SPACE_WITHIN_BRACES = false;
    public boolean CONDITION_SPACE_AFTER_SEMICOLON = true;
    public boolean CONDITION_SPACE_BEFORE_SEMICOLON = false;

    // Tail trim
    public boolean IMPORT_TRIM_TAIL = true;
    public boolean CONTEXT_TRIM_TAIL = true;

    // Assignment
    public boolean SPACE_AROUND_ASSIGNMENT_OPERATORS = false;

    // Operators
    public boolean SPACE_AROUND_OPERATOR_ORDER = false;
    public boolean SPACE_AROUND_OPERATOR_EQUALITY = false;
    public boolean SPACE_AROUND_OPERATOR_ARITHMETIC = false;
    public boolean SPACE_AROUND_OPERATOR_WEIGHT = false;
    public boolean SPACE_AROUND_OPERATOR_OTHERS = false;

    protected QCodeStyleSettings(CodeStyleSettings container) {
        super("QCodeStyleSettings", container);
    }
}
