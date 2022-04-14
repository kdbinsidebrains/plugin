package org.kdb.inside.brains.lang.formatting;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class QCodeStyleSettings extends CustomCodeStyleSettings {
    // Lambda
    public int LAMBDA_PARAMS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean LAMBDA_ALIGN_BRACE = false;
    public boolean LAMBDA_SPACE_AFTER_PARAMETERS = true;
    public boolean LAMBDA_SPACE_WITHIN_BRACES = false;
    public boolean LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE = true;
    public boolean LAMBDA_PARAMS_ALIGN_NAMES = true;
    public boolean LAMBDA_PARAMS_ALIGN_BRACKETS = true;
    public boolean LAMBDA_PARAMS_LBRACKET_ON_NEXT_LINE = false;
    public boolean LAMBDA_PARAMS_RBRACKET_ON_NEXT_LINE = false;
    public boolean LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS = false;
    public boolean LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON = false;
    public boolean LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON = false;

    // Controls
    public int CONTROL_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONTROL_ALIGN_EXPRS = true;
    public boolean CONTROL_ALIGN_BRACKET = true;
    public boolean CONTROL_LBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_RBRACKET_ON_NEXT_LINE = false;
    public boolean CONTROL_SPACE_AFTER_OPERATOR = false;
    public boolean CONTROL_SPACE_WITHIN_BRACES = false;
    public boolean CONTROL_SPACE_AFTER_SEMICOLON = true;
    public boolean CONTROL_SPACE_BEFORE_SEMICOLON = false;

    // Conditions
    public int CONDITION_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean CONDITION_ALIGN_EXPRS = true;
    public boolean CONDITION_ALIGN_BRACKET = true;
    public boolean CONDITION_LBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_RBRACKET_ON_NEXT_LINE = false;
    public boolean CONDITION_SPACE_AFTER_OPERATOR = false;
    public boolean CONDITION_SPACE_WITHIN_BRACES = false;
    public boolean CONDITION_SPACE_AFTER_SEMICOLON = true;
    public boolean CONDITION_SPACE_BEFORE_SEMICOLON = false;

    // Execution
    public boolean EXECUTION_SPACE_BEFORE_ARGUMENTS = false;
    public boolean EXECUTION_SPACE_BEFORE_SYMBOLS = true;
    public boolean EXECUTION_SPACE_BEFORE_PARAMETER = true;
    public boolean EXECUTION_SPACE_AFTER_INTERNAL = false;

    // Arguments
    public int ARGUMENTS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean ARGUMENTS_ALIGN_EXPRS = true;
    public boolean ARGUMENTS_ALIGN_BRACKET = true;
    public boolean ARGUMENTS_LBRACKET_ON_NEXT_LINE = false;
    public boolean ARGUMENTS_RBRACKET_ON_NEXT_LINE = false;
    public boolean ARGUMENTS_SPACE_WITHIN_BRACES = false;
    public boolean ARGUMENTS_SPACE_AFTER_SEMICOLON = true;
    public boolean ARGUMENTS_SPACE_BEFORE_SEMICOLON = false;

    // Grouping
    public int GROUPING_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean GROUPING_ALIGN_EXPRS = true;
    public boolean GROUPING_ALIGN_BRACKET = false;
    public boolean GROUPING_LBRACKET_ON_NEXT_LINE = false;
    public boolean GROUPING_RBRACKET_ON_NEXT_LINE = false;
    public boolean GROUPING_SPACE_WITHIN_BRACES = false;
    public boolean GROUPING_SPACE_AFTER_SEMICOLON = true;
    public boolean GROUPING_SPACE_BEFORE_SEMICOLON = false;

    // Parentheses
    public int PARENTHESES_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean PARENTHESES_ALIGN_EXPRS = true;
    public boolean PARENTHESES_ALIGN_PAREN = false;
    public boolean PARENTHESES_LPAREN_ON_NEXT_LINE = false;
    public boolean PARENTHESES_RPAREN_ON_NEXT_LINE = false;
    public boolean PARENTHESES_SPACE_WITHIN_PARENS = false;
    public boolean PARENTHESES_SPACE_AFTER_SEMICOLON = false;
    public boolean PARENTHESES_SPACE_BEFORE_SEMICOLON = false;

    // Assignment
    public boolean SPACE_AROUND_ASSIGNMENT_OPERATORS = false;

    // Operators
    public boolean SPACE_AFTER_OPERATOR_COMMA = false;
    public boolean SPACE_AROUND_OPERATOR_CUT = false;
    public boolean SPACE_AROUND_OPERATOR_ORDER = false;
    public boolean SPACE_AROUND_OPERATOR_WEIGHT = true;
    public boolean SPACE_AROUND_OPERATOR_EQUALITY = false;
    public boolean SPACE_AROUND_OPERATOR_ARITHMETIC = false;
    public boolean SPACE_AROUND_OPERATOR_OTHERS = false;

    // Iterators
    public boolean ITERATOR_SPACE_AROUND = false;
    public boolean ITERATOR_SPACE_BETWEEN = false;
    public boolean ITERATOR_SPACE_AFTER_OPERATOR = false;

    // Mode
    public int MODE_WRAP_TYPE = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean MODE_ALIGN = true;
    public boolean MODE_SPACE_AFTER = true;

    // Query
    public int QUERY_WRAP_PARTS = CommonCodeStyleSettings.DO_NOT_WRAP;
    public int QUERY_WRAP_COLUMNS = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean QUERY_PARTS_ALIGN = true;
    public boolean QUERY_COLUMNS_ALIGN = true;
    public boolean QUERY_SPACE_AFTER_COMMA = true;

    // Table
    public int TABLE_COLUMNS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP;
    public boolean TABLE_ALIGN_BRACKETS = true;
    public boolean TABLE_ALIGN_PARENS = false;
    public boolean TABLE_ALIGN_COLUMNS = true;
    public boolean TABLE_LBRACKET_NEW_LINE = true;
    public boolean TABLE_RBRACKET_NEW_LINE = true;
    public boolean TABLE_SPACE_AFTER_KEY_SEMICOLON = true;
    public boolean TABLE_SPACE_BEFORE_KEY_SEMICOLON = false;
    public boolean TABLE_SPACE_BEFORE_COLUMNS = true;
    public boolean TABLE_SPACE_AFTER_COLUMN_SEMICOLON = true;
    public boolean TABLE_SPACE_BEFORE_COLUMN_SEMICOLON = false;
    public boolean TABLE_CLOSE_PAREN_NEW_LINE = true;
    public boolean TABLE_SPACE_AFTER_COLUMNS = false;
    public boolean TABLE_SPACE_BEFORE_GLOBAL_CLOSE_BRACKET = true;

    // Other
    public boolean SEMICOLON_SPACE_AFTER = true;
    public boolean RETURN_SPACE_AFTER_COLON = false;
    public boolean SIGNAL_SPACE_AFTER_SIGNAL = false;
    public boolean EXPRESSION_SEMICOLON_TRIM_SPACES = true;
    public boolean EXPRESSION_SEMICOLON_REMOVE_LINES = true;
    public boolean FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL = false;

    public boolean INVOKE_ALIGN_ITEMS = true;

    // Tail trim
    public boolean IMPORT_TRIM_TAIL = true;
    public boolean CONTEXT_TRIM_TAIL = true;

    protected QCodeStyleSettings(CodeStyleSettings container) {
        super("QCodeStyleSettings", container);
    }
}
