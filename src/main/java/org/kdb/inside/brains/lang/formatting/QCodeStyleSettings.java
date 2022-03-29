package org.kdb.inside.brains.lang.formatting;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class QCodeStyleSettings extends CustomCodeStyleSettings {
    // Params
    public int PARAMS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean PARAMS_ALIGN_VARS = true;
    public boolean PARAMS_ALIGN_BRACKET = true;
    public boolean PARAMS_LBRACKET_ON_NEXT_LINE = false;
    public boolean PARAMS_RBRACKET_ON_NEXT_LINE = false;
    public boolean PARAMS_SPACE_WITHIN_BRACKET = false;
    public boolean PARAMS_SPACE_AFTER_SEMICOLON = true;
    public boolean PARAMS_SPACE_BEFORE_SEMICOLON = false;

    // Lambda
    public boolean LAMBDA_ALIGN_BRACE = false;
    public boolean LAMBDA_SPACE_AFTER_PARAMETERS = true;
    public boolean LAMBDA_SPACE_BEFORE_CLOSE_BRACE = true;
    public boolean LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE = true;

    // Controls
    public int CONTROL_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONTROL_ALIGN_EXPRS = true;
    public boolean CONTROL_ALIGN_BRACKET = false;
    public boolean CONTROL_LBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_RBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_SPACE_AFTER_OPERATOR = false;
    public boolean CONTROL_SPACE_WITHIN_BRACES = false;
    public boolean CONTROL_SPACE_AFTER_SEMICOLON = true;
    public boolean CONTROL_SPACE_BEFORE_SEMICOLON = false;
    public boolean CONTROL_SPACE_BEFORE_EXECUTION = false;

    // Conditions
    public int CONDITION_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONDITION_ALIGN_EXPRS = true;
    public boolean CONDITION_ALIGN_BRACKET = false;
    public boolean CONDITION_LBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_RBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_SPACE_AFTER_OPERATOR = false;
    public boolean CONDITION_SPACE_WITHIN_BRACES = false;
    public boolean CONDITION_SPACE_AFTER_SEMICOLON = true;
    public boolean CONDITION_SPACE_BEFORE_SEMICOLON = false;

    // Other
    public boolean RETURN_SPACE_AFTER_COLON = false;
    public boolean SIGNAL_SPACE_AFTER_SIGNAL = false;
    public boolean EXPRESSION_SEMICOLON_TRIM_SPACES = true;
    public boolean EXPRESSION_SEMICOLON_REMOVE_LINES = true;

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


    // Tables
    public int TABLE_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean TABLE_WRAP_ALIGN = true;
    public boolean TABLE_SPACE_AFTER_KEY_COLUMNS = true;
    public boolean TABLE_KEYS_EMPTY_LINE = true;
    public boolean TABLE_CPAREN_EMPTY_LINE = true;

    protected QCodeStyleSettings(CodeStyleSettings container) {
        super("QCodeStyleSettings", container);
    }
}
