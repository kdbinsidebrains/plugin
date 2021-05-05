package org.kdb.inside.brains;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.Stack;

import static org.kdb.inside.brains.psi.QTypes.*;

%%

%public %class QLexer
%implements com.intellij.lexer.FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return LINE_BREAK;
%eof}

%{
    private final Stack<Integer> stateStack = new Stack<>();

    protected void resetState() {
        stateStack.clear();
    }

    protected void yybeginstate(int... states) {
        assert states.length > 0;
        for (int state : states) {
            assert state != YYINITIAL;
            stateStack.push(state);
            yybegin(state);
        }
    }

    protected void yyendstate(int... states) {
        for (int state : states) {
            assert state != YYINITIAL;
            assert !stateStack.isEmpty() : stateStack;
            int previous = stateStack.pop();
            assert previous == state : "States does not match: previous=" + previous + ", expected=" + state;
        }
        yybegin(stateStack.isEmpty() ? YYINITIAL : stateStack.peek());
    }
%}

// Patterns
LINE_SPACE=[\ \t\f]
LINE_BREAK=\r|\n|\r\n|<<eof>>
WHITE_SPACE=({LINE_SPACE}|{LINE_BREAK})*{LINE_SPACE}+

// Changing anything - don't forget update QLanguage
OPERATOR=[!%&#,-<=>@_~\?\.\*\^\|\$\+]
ITERATOR=("/" | \\ | ' | "/": | \\: | ':)+
MODE_PATTERN=[a-zA-Z]")"
FILE_PATH_PATTERN=[^|?*<\">+\[\]'\n\r\ \t\f]+
VARIABLE_PATTERN=[\.a-zA-Z][\._a-zA-Z0-9]*
COMMAND_ARGS_PATTERN=[^\ \t\f\r\n<<eof>>][^\r\n<<eof>>]*

// Keywords and system commands
// Changing anything - don't forget update QLanguage
CONTROL_PATTERN=(if|do|while)
CONDITION_PATTERN=":"|"?"|"$"|"@"|"." // ":" is from k3
// QSQL supporting
QUERY_TYPE=(select|exec|update|delete)
QUERY_BY=(by)
QUERY_FROM=(from)

FUNCTION=(set|get|abs|acos|aj|aj0|all|and|any|asc|asin|asof|atan|attr|avg|avgs|bin|binr|by|ceiling|cols|cor|cos|count
       |cov|cross|csv|cut|deltas|desc|dev|differ|distinct|div|dsave|each|ej|ema|enlist|eval|except
       |exit|exp|fby|fills|first|fkeys|flip|floor|getenv|group|gtime|hclose|hcount|hdel|hopen
       |hsym|iasc|idesc|ij|in|insert|inter|inv|key|keys|last|like|lj|ljf|load|log|lower|lsq|ltime|ltrim|mavg
       |max|maxs|mcount|md5|mdev|med|meta|min|mins|mmax|mmin|mmu|mod|msum|neg|next|not|null|or|over|parse
       |peach|pj|prd|prds|prev|prior|rand|rank|ratios|raze|read0|read1|reciprocal|reverse|rload|rotate|rsave
       |rtrim|save|scan|scov|sdev|setenv|show|signum|sin|sqrt|ss|ssr|string|sublist|sum|sums|sv
       |svar|system|tables|tan|til|trim|type|uj|ungroup|union|upper|upsert|value|var|view|views|vs
       |wavg|within|where|wj|wj1|wsum|ww|xasc|xbar|xcol|xcols|xdesc|xexp|xgroup|xkey|xlog|xprev|xrank|ujf|reval)

COMMAND_PATTERN="\\"(\w+|[12\\])
//COMMAND_PATTERN="\\"([abBcCdeEfglopPrsStTuvwWxz12_\\]|ts|cd|\w+)

COMMENT_LINE="/" [^\r\n]+ {LINE_BREAK}?
COMMENT_REST={WHITE_SPACE}+ "/" [^\r\n]* {LINE_BREAK}?

TYPE_CAST_PATTERN=(("`"\w*)|("\""\w*"\""))"$"

INT_CODE=[ihjfepnuvt]
FLOAT_CODE=[fe]
TIME_CODE=[uvt]
DATETIME_CODE=[mdz]
TIMESTAMP_CODE=[pn]
GUID_CODE="g"

NULL=0[Nn]
INFINITY=-?0[iIwW]

BYTE_CHAR=[[:digit:]A-Fa-f]
BYTE="0x"{BYTE_CHAR}{BYTE_CHAR}?
BYTE_LIST=("0x"{BYTE_CHAR}{2}{BYTE_CHAR}+)

BOOLEAN=[01]"b"
BOOLEAN_LIST=([01][01]+"b")

INTEGER=-?(0|[1-9][0-9]*)
INTEGER_ITEM=({INTEGER}|{NULL}|{INFINITY})
INTEGER_LIST=({INTEGER_ITEM}({WHITE_SPACE}{INTEGER_ITEM})+{INT_CODE}?)

FLOAT_ALL={INTEGER}(\.[0-9]*)|(\.[0-9]+)
FLOAT_EXP={INTEGER}(\.[0-9]+)?([eE][+-]?[0-9]*)
FLOAT_ITEM={INTEGER}|{FLOAT_ALL}|{FLOAT_EXP}|{NULL}|{INFINITY}
FLOAT_LIST=({FLOAT_ITEM}({WHITE_SPACE}{FLOAT_ITEM})+{FLOAT_CODE}?)

MONTH=[:digit:]{4}\.[:digit:]{2}
MONTH_ITEM={MONTH}|{NULL}|{INFINITY}
MONTH_LIST=({MONTH_ITEM}({WHITE_SPACE}{MONTH_ITEM})+"m"?)

TIME=[:digit:]+(\:[:digit:]{2}(\:[0-5][:digit:](\.[:digit:]+)?)?)?
TIME_ITEM={INTEGER}|{NULL}|{MINUTE}|{SECOND}|{TIME}|{INFINITY}
TIME_LIST=({TIME_ITEM}({WHITE_SPACE}{TIME_ITEM})+{TIME_CODE}?)

DATE={MONTH}\.[:digit:]{2}
DATE_ITEM={DATE}|{NULL}|{INFINITY}
DATE_LIST=({DATE_ITEM}({WHITE_SPACE}{DATE_ITEM})+"d"?)

DATETIME={DATE}"T"{TIME}
DATETIME_ITEM={DATETIME}|{DATE}|{NULL}|{INFINITY}
DATETIME_LIST=({DATETIME_ITEM}({WHITE_SPACE}{DATETIME_ITEM})+"z"?)

TIMESTAMP=-?{DATE}("D"{TIME})?
TIMESPAN={INTEGER}"D"({TIME})?
TIMESTAMP_ITEM={TIMESTAMP}|{TIMESPAN}|{FLOAT_ALL}|{TIME_ITEM}
TIMESTAMP_LIST=({TIMESTAMP_ITEM}({WHITE_SPACE}{TIMESTAMP_ITEM})+{TIMESTAMP_CODE}?)

MINUTE=[:digit:]{2,3}\:[0-5][:digit:]
MINUTE_LIST=({MINUTE}({WHITE_SPACE}{MINUTE})+)

SECOND={MINUTE}\:[0-5][:digit:]
SECOND_LIST=({SECOND}({WHITE_SPACE}{SECOND})+)

CH=([^\\\"]|\\[^\ \t])
CHAR=\"{CH}\"
STRING=(\"\"|\"{CH}{CH}+\")

SYMBOL_PATTERN="`"([.:/_a-zA-Z0-9]+)?
SYMBOL_ITERATOR=(\\ | ' | \\: | ':)+

ATOM={BOOLEAN}|{BYTE}|
    {INTEGER}{INT_CODE}?|
    ({FLOAT_ALL}|{FLOAT_EXP}){FLOAT_CODE}?|
    ({INFINITY}|{NULL})({INT_CODE}|{DATETIME_CODE})?|
    {NULL}{GUID_CODE}|
    ({TIMESTAMP}|{TIMESPAN}|{FLOAT_ALL}){TIMESTAMP_CODE}?|
    ({MINUTE}|{SECOND}|{TIME}){TIME_CODE}?|
    {DATE}[dz]?|
    {DATETIME}"z"?|
    {MONTH}"m"?

ATOMS={BOOLEAN_LIST}|{BYTE_LIST}|{INTEGER_LIST}|{FLOAT_LIST}|
    {TIMESTAMP_LIST}|{TIME_LIST}|{MONTH_LIST}|{DATE_LIST}|{DATETIME_LIST}|{MINUTE_LIST}|{SECOND_LIST}

%state MODE_STATE
%state ITERATOR_STATE
%state COMMAND_IMPORT_STATE
%state COMMAND_CONTEXT_STATE
%state COMMAND_SYSTEM_STATE
%state COMMENT_ALL
%state COMMENT_BLOCK
%state COMMENT_INLINE
%state DROP_CUT_STATE
%state QUERY_STATE
%state LAMBDA_STATE

%%

<COMMAND_IMPORT_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {FILE_PATH_PATTERN}                         { return FILE_PATH_PATTERN; }
  {LINE_BREAK}                                { yybegin(YYINITIAL); return LINE_BREAK; }
}

<COMMAND_CONTEXT_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {VARIABLE_PATTERN}                          { return VARIABLE_PATTERN;}
  {LINE_BREAK}                                { yybegin(YYINITIAL); return LINE_BREAK; }
}

<COMMAND_SYSTEM_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {COMMAND_ARGS_PATTERN}                      { return COMMAND_ARGUMENTS;}
  {LINE_BREAK}                                { yybegin(YYINITIAL); return LINE_BREAK; }
}

<ITERATOR_STATE> {
  {ITERATOR}                                  { yybegin(YYINITIAL); return ITERATOR;}
}

<DROP_CUT_STATE> {
  "_"/{ITERATOR}                              { yybegin(ITERATOR_STATE); return OPERATOR;}
  "_"                                         { yybegin(YYINITIAL); return OPERATOR;}
}

<COMMENT_INLINE> {
  {COMMENT_LINE}                              { yybegin(YYINITIAL); return COMMENT;}
}

<COMMENT_BLOCK> {
  ^"\\"{LINE_BREAK}                           { yybegin(YYINITIAL); return COMMENT; }
  {LINE_BREAK}+                               { return COMMENT; }
  .*                                          { return COMMENT; }
}

<COMMENT_ALL> {
  {LINE_BREAK}+                               { return COMMENT; }
  .*                                          { return COMMENT; }
}

<QUERY_STATE> {
  ","                                         {return COMMA; }
}

// NOT MIGRATED
<YYINITIAL, QUERY_STATE, LAMBDA_STATE> {
  "("{LINE_SPACE}*")"                         { return ATOMS; }
  "("{LINE_SPACE}*"::"{LINE_SPACE}*")"        { return NILL; }

  "("                                         { return PAREN_OPEN; }
  ")"/{ITERATOR}                              { yybegin(ITERATOR_STATE); return PAREN_CLOSE; }
  ")"/"_"                                     { yybegin(DROP_CUT_STATE); return PAREN_CLOSE; }
  ")"                                         { return PAREN_CLOSE; }
  ";"/{COMMENT_LINE}                          { yybegin(COMMENT_INLINE); return SEMICOLON; }
  ";"                                         { return SEMICOLON; }
  "["                                         { return BRACKET_OPEN; }
  "]"/{ITERATOR}                              { yybegin(ITERATOR_STATE); return BRACKET_CLOSE; }
  "]"/"_"                                     { yybegin(DROP_CUT_STATE); return BRACKET_CLOSE; }
  "]"                                         { return BRACKET_CLOSE; }
  "{"                                         { yybeginstate(LAMBDA_STATE); return BRACE_OPEN; }
  "}"/{ITERATOR}                              { yyendstate(LAMBDA_STATE); yybegin(ITERATOR_STATE); return BRACE_CLOSE; }
  "}"                                         { yyendstate(LAMBDA_STATE); return BRACE_CLOSE; }

  ":"/{ITERATOR}                              { yybegin(ITERATOR_STATE); return COLON; }
  ":"                                         { return COLON; }
  "'"                                         { return SIGNAL_PATTERN; }

  {LINE_BREAK}+                               { return LINE_BREAK; }

  ^"/"{LINE_BREAK}                            { yybegin(COMMENT_BLOCK); return COMMENT; }
  ^"\\"{LINE_BREAK}                           { yybegin(COMMENT_ALL);   return COMMENT; }
  ^{COMMENT_LINE}                             { return COMMENT; }
  {COMMENT_REST}/{LINE_BREAK}                 { return COMMENT; }
  {COMMENT_REST}                              { return COMMENT; }

  "\\"                                        { return TRACE; }

  "-"/-[0-9]                                  { return OPERATOR;} // --6 -> 6

  ^"\\l"/{LINE_SPACE}+!{LINE_BREAK}           { yybegin(COMMAND_IMPORT_STATE);  return COMMAND_IMPORT; }

  ^"\\d"/{LINE_SPACE}+!{LINE_BREAK}           { yybegin(COMMAND_CONTEXT_STATE); return COMMAND_CONTEXT; }
  ^"\\d"/{LINE_BREAK}                         { return COMMAND_CONTEXT; }
  ^"\\d"/{WHITE_SPACE}                        { return COMMAND_CONTEXT; }

  ^{COMMAND_PATTERN}/{LINE_SPACE}+!{LINE_BREAK} { yybegin(COMMAND_SYSTEM_STATE); return COMMAND_SYSTEM; }
  ^{COMMAND_PATTERN}/{LINE_BREAK}               { return COMMAND_SYSTEM; }
  ^{COMMAND_PATTERN}/{WHITE_SPACE}              { return COMMAND_SYSTEM; }

  ^{MODE_PATTERN}                             { return MODE_PATTERN; }

  {TYPE_CAST_PATTERN}                         { return TYPE_CAST_PATTERN; }

  [0-6]":"/{ITERATOR}                         { yybegin(ITERATOR_STATE); return OPERATOR; }
  [0-6]":"/[^\[]                              { return OPERATOR; }

  {CONTROL_PATTERN}/{WHITE_SPACE}*"["         { return CONTROL_PATTERN; }
  {CONDITION_PATTERN}/{WHITE_SPACE}*"["       { return CONDITION_PATTERN; }

  {SYMBOL_PATTERN}/{SYMBOL_ITERATOR}          { yybegin(ITERATOR_STATE); return SYMBOL_PATTERN; }
  {SYMBOL_PATTERN}                            { return SYMBOL_PATTERN; }
  {WHITE_SPACE}                               { return TokenType.WHITE_SPACE; }

  {QUERY_TYPE}                                { yybeginstate(QUERY_STATE); return QUERY_TYPE; }
  {QUERY_BY}                                  { return QUERY_BY; }
  {QUERY_FROM}                                { yyendstate(QUERY_STATE); return QUERY_FROM; }

  {FUNCTION}/{ITERATOR}                       { yybegin(ITERATOR_STATE); return FUNCTION; }
  {FUNCTION}                                  { return FUNCTION; }

  {ATOM}/{ITERATOR}                           { yybegin(ITERATOR_STATE); return ATOM; }
  {ATOM}/"_"                                  { yybegin(DROP_CUT_STATE); return ATOM; }
  {ATOM}                                      { return ATOM; }
  {ATOMS}/{ITERATOR}                          { yybegin(ITERATOR_STATE); return ATOMS; }
  {ATOMS}/"_"                                 { yybegin(DROP_CUT_STATE); return ATOMS; }
  {ATOMS}                                     { return ATOMS; }
  {CHAR}/{ITERATOR}                           { yybegin(ITERATOR_STATE); return CHAR; }
  {CHAR}                                      { return CHAR; }
  {STRING}/{ITERATOR}                         { yybegin(ITERATOR_STATE); return STRING; }
  {STRING}/"_"                                { yybegin(DROP_CUT_STATE); return STRING; }
  {STRING}                                    { return STRING; }

  {VARIABLE_PATTERN}/{ITERATOR}               { yybegin(ITERATOR_STATE); return VARIABLE_PATTERN; }
  {VARIABLE_PATTERN}                          { return VARIABLE_PATTERN; }

  {OPERATOR}/{ITERATOR}                       { yybegin(ITERATOR_STATE); return OPERATOR;}
  {OPERATOR}                                  { return OPERATOR;}
}

[^] { return TokenType.BAD_CHARACTER; }
