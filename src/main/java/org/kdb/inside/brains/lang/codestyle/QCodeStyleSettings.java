package org.kdb.inside.brains.lang.codestyle;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class QCodeStyleSettings extends CustomCodeStyleSettings {
    // Lambda
    public boolean LAMBDA_SPACE_AFTER_PARAMETERS = true;
    public boolean LAMBDA_SPACE_BEFORE_BRACE_CLOSE = true;

    // Controls
    public int CONTROL_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONTROL_WRAP_ALIGN = true;
    public boolean CONTROL_OBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_CBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_SPACE_AFTER_OPERATOR = false;
    public boolean CONTROL_SPACE_WITHIN_BRACES = false;
    public boolean CONTROL_SPACE_AFTER_SEMICOLON = true;
    public boolean CONTROL_SPACE_BEFORE_SEMICOLON = false;

    // Conditions
    public int CONDITION_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONDITION_WRAP_ALIGN = true;
    public boolean CONDITION_OBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_CBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_SPACE_AFTER_OPERATOR = false;
    public boolean CONDITION_SPACE_WITHIN_BRACES = false;
    public boolean CONDITION_SPACE_AFTER_SEMICOLON = true;
    public boolean CONDITION_SPACE_BEFORE_SEMICOLON = false;

    // Tables
    public int TABLE_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean TABLE_WRAP_ALIGN = true;
    public boolean TABLE_SPACE_AFTER_KEY_COLUMNS = true;
    public boolean TABLE_KEYS_EMPTY_LINE = true;
    public boolean TABLE_CPAREN_EMPTY_LINE = true;

    // Expressions
    public boolean EXPRESSION_SEMICOLON_ON_NEW_LINE = false;

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
