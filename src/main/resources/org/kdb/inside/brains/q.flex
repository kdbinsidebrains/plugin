package org.kdb.inside.brains;

import com.intellij.psi.tree.IElementType;

import java.util.Stack;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.kdb.inside.brains.psi.QTypes.*;

%%

%public %class _QLexer
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

    protected void yybeginstate(int state) {
        assert state != YYINITIAL;
        stateStack.push(state);
        yybegin(state);
    }

    protected void yyendstate(int state) {
        assert state != YYINITIAL;
        assert !stateStack.isEmpty() : stateStack;
        int previous = stateStack.pop();
        assert previous == state : "States does not match: previous=" + previous + ", expected=" + state;
        yybegin(stateStack.isEmpty() ? YYINITIAL : stateStack.peek());
    }
%}

// Patterns
LINE_SPACE=[\ \t\f]
LINE_BREAK=\r\n|\r|\n|<<eof>>
WHITE_SPACE=({LINE_SPACE}|{LINE_BREAK})*{LINE_SPACE}+

// Changing anything - don't forget update QLanguage
OPERATOR=[!%&#,-<=>@_~\?\.\*\^\|\$\+]
ITERATOR=("/": | \\: | ': | "/" | \\ | ' )+

MODE_PATTERN=[a-zA-Z]")"
ITERATOR_PATTERN={ITERATOR}
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

// (abs x) or abs[x]
UNARY_FUNCTION=(abs|all|any|asc|iasc|attr|avg|avgs|ceiling|count|
    cols|cos|acos|deltas|desc|idesc|dev|sdev|differ|distinct|
    enlist|eval|reval|exit|exp|first|last|fkeys|flip|floor|
    get|getenv|group|gtime|ltime|hcount|hdel|hopen|hclose|hsym|
    inv|key|keys|load|log|lower|upper|max|maxs|md5|med|meta|min|
    mins|neg|next|prev|not|null|parse|prd|prds|rand|rank|ratios|
    raze|read0|read1|reciprocal|reverse|save|rsave|show|signum|sin|
    asin|sqrt|string|sum|sums|system|tables|tan|atan|til|trim|ltrim|rtrim|
    type|value|var|svar|view|views|where)

// and[x;y] or (x and y)
BINARY_FUNCTION=(and|xasc|asof|mavg|wavg|bin|binr|mcount|xcol|
    xcols|cor|cov|scov|cross|csv|xdesc|mdev|div|dsave|each|peach|ema|except|xexp|fby|set|setenv|
    ij|ijf|in|insert|inter|xkey|like|lj|ljf|xlog|lsq|mmax|mmin|mmu|mod|xprev|or|over|scan|pj|prior|
    rotate|ss|sublist|msum|wsum|sv|uj|ujf|union|ungroup|upsert|vs|within|xbar|xgroup|xrank)

// ej[c;t1;t2]
COMPLEX_FUNCTION=(aj|aj0|ajf|ajf0|ej|ssr|wj|wj1)

COMMAND_PATTERN="\\"(\w+|[12\\])
//COMMAND_PATTERN="\\"([abBcCdeEfglopPrsStTuvwWxz12_\\]|ts|cd|\w+)

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

VECTOR={BOOLEAN_LIST}|{BYTE_LIST}|{INTEGER_LIST}|{FLOAT_LIST}|
    {TIMESTAMP_LIST}|{TIME_LIST}|{MONTH_LIST}|{DATE_LIST}|{DATETIME_LIST}|{MINUTE_LIST}|{SECOND_LIST}

%state MODE_STATE
%state ITERATOR_STATE
%state COMMAND_IMPORT_STATE
%state COMMAND_CONTEXT_STATE
%state COMMAND_SYSTEM_STATE
%state COMMENT_ALL_STATE
%state COMMENT_BLOCK_STATE
%state DROP_CUT_STATE
%state LAMBDA_STATE
%state QUERY_STATE

%%

<COMMAND_IMPORT_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {FILE_PATH_PATTERN}                         { return FILE_PATH_PATTERN; }
  {LINE_BREAK}                                { yyendstate(COMMAND_IMPORT_STATE); return LINE_BREAK; }
}

<COMMAND_CONTEXT_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {VARIABLE_PATTERN}                          { return VARIABLE_PATTERN;}
  {LINE_BREAK}                                { yyendstate(COMMAND_CONTEXT_STATE); return LINE_BREAK; }
}

<COMMAND_SYSTEM_STATE> {
  {LINE_SPACE}                                { return LINE_SPACE; }
  {COMMAND_ARGS_PATTERN}                      { return COMMAND_ARGUMENTS;}
  {LINE_BREAK}                                { yyendstate(COMMAND_SYSTEM_STATE); return LINE_BREAK; }
}

<ITERATOR_STATE> {
  {ITERATOR}                                  { yyendstate(ITERATOR_STATE); return ITERATOR;}
}

<DROP_CUT_STATE> {
  "_"/{ITERATOR_PATTERN}                      { yyendstate(DROP_CUT_STATE); yybeginstate(ITERATOR_STATE); return OPERATOR;}
  "_"                                         { yyendstate(DROP_CUT_STATE); return OPERATOR;}
}

<LAMBDA_STATE> {
  "}"/{ITERATOR_PATTERN}                      { yyendstate(LAMBDA_STATE); yybeginstate(ITERATOR_STATE); return BRACE_CLOSE; }
  "}"                                         { yyendstate(LAMBDA_STATE); return BRACE_CLOSE; }
 }

<QUERY_STATE> {
 ","                                          { return COMMA; }
 {QUERY_BY}                                   { return QUERY_BY; }
 {QUERY_FROM}                                 { yyendstate(QUERY_STATE); return QUERY_FROM; }
}

<COMMENT_ALL_STATE> {
  .*{LINE_BREAK}?                             { return BLOCK_COMMENT; }
}

<COMMENT_BLOCK_STATE> {
  ^"\\"/{LINE_BREAK}+                        { yyendstate(COMMENT_BLOCK_STATE); return BLOCK_COMMENT; }
  .*{LINE_BREAK}?                            { return BLOCK_COMMENT; }
}

// NOT MIGRATED
<YYINITIAL, LAMBDA_STATE, QUERY_STATE> {
  "("{LINE_SPACE}*")"                         { return VECTOR; }
  "("{LINE_SPACE}*"::"{LINE_SPACE}*")"        { return NILL; }

  "("                                         { return PAREN_OPEN; }
  ")"/{ITERATOR_PATTERN}                      { yybeginstate(ITERATOR_STATE); return PAREN_CLOSE; }
  ")"/"_"                                     { yybeginstate(DROP_CUT_STATE); return PAREN_CLOSE; }
  ")"                                         { return PAREN_CLOSE; }
  ";"                                         { return SEMICOLON; }
  "["                                         { return BRACKET_OPEN; }
  "]"/{ITERATOR_PATTERN}                      { yybeginstate(ITERATOR_STATE); return BRACKET_CLOSE; }
  "]"/"_"                                     { yybeginstate(DROP_CUT_STATE); return BRACKET_CLOSE; }
  "]"                                         { return BRACKET_CLOSE; }

  "{"                                         { yybeginstate(LAMBDA_STATE); return BRACE_OPEN; }

  ^"/".*                                      { return LINE_COMMENT; }
  {LINE_SPACE}+"/".*                          { return LINE_COMMENT; }
  {LINE_BREAK}+/{LINE_SPACE}*"/"              { return WHITE_SPACE; }
  ^"\\"/{LINE_BREAK}                          { yybeginstate(COMMENT_ALL_STATE); return BLOCK_COMMENT; }
  ^"/"/{LINE_BREAK}                           { yybeginstate(COMMENT_BLOCK_STATE); return BLOCK_COMMENT; }

  {LINE_BREAK}+                               { return LINE_BREAK; }

  "\\"                                        { return TRACE; }

  ":"/{ITERATOR_PATTERN}                      { yybeginstate(ITERATOR_STATE); return COLON; }
  ":"                                         { return COLON; }
   {ITERATOR_PATTERN}                         { return ITERATOR; }

  "-"/-[0-9]                                  { return OPERATOR;} // --6 -> 6

  ^"\\l"/{LINE_SPACE}+!{LINE_BREAK}           { yybeginstate(COMMAND_IMPORT_STATE);  return COMMAND_IMPORT; }

  ^"\\d"/{LINE_SPACE}+!{LINE_BREAK}           { yybeginstate(COMMAND_CONTEXT_STATE); return COMMAND_CONTEXT; }
  ^"\\d"/{LINE_BREAK}                         { return COMMAND_CONTEXT; }
  ^"\\d"/{WHITE_SPACE}                        { return COMMAND_CONTEXT; }

  ^{COMMAND_PATTERN}/{LINE_SPACE}+!{LINE_BREAK} { yybeginstate(COMMAND_SYSTEM_STATE); return COMMAND_SYSTEM; }
  ^{COMMAND_PATTERN}/{LINE_BREAK}               { return COMMAND_SYSTEM; }
  ^{COMMAND_PATTERN}/{WHITE_SPACE}              { return COMMAND_SYSTEM; }

  ^{MODE_PATTERN}                             { return MODE_PATTERN; }

  {TYPE_CAST_PATTERN}                         { return TYPE_CAST_PATTERN; }

  [0-6]":"/{ITERATOR_PATTERN}                 { yybeginstate(ITERATOR_STATE); return OPERATOR; }
  [0-6]":"/[^\[]                              { return OPERATOR; }

  {CONTROL_PATTERN}/{WHITE_SPACE}*"["         { return CONTROL_PATTERN; }
  {CONDITION_PATTERN}/{WHITE_SPACE}*"["       { return CONDITION_PATTERN; }

  {SYMBOL_PATTERN}/{SYMBOL_ITERATOR}          { yybeginstate(ITERATOR_STATE); return SYMBOL_PATTERN; }
  {SYMBOL_PATTERN}                            { return SYMBOL_PATTERN; }
  {WHITE_SPACE}                               { return WHITE_SPACE; }

  {QUERY_TYPE}                                { yybeginstate(QUERY_STATE); return QUERY_TYPE; }

  {UNARY_FUNCTION}/{ITERATOR_PATTERN}         { yybeginstate(ITERATOR_STATE); return UNARY_FUNCTION; }
  {UNARY_FUNCTION}                            { return UNARY_FUNCTION; }

  {BINARY_FUNCTION}/{ITERATOR_PATTERN}        { yybeginstate(ITERATOR_STATE); return BINARY_FUNCTION; }
  {BINARY_FUNCTION}                           { return BINARY_FUNCTION; }

  {COMPLEX_FUNCTION}/{ITERATOR_PATTERN}       { yybeginstate(ITERATOR_STATE); return COMPLEX_FUNCTION; }
  {COMPLEX_FUNCTION}                          { return COMPLEX_FUNCTION; }

  {ATOM}/{ITERATOR_PATTERN}                   { yybeginstate(ITERATOR_STATE); return ATOM; }
  {ATOM}/"_"                                  { yybeginstate(DROP_CUT_STATE); return ATOM; }
  {ATOM}                                      { return ATOM; }
  {VECTOR}/{ITERATOR_PATTERN}                 { yybeginstate(ITERATOR_STATE); return VECTOR; }
  {VECTOR}/"_"                                { yybeginstate(DROP_CUT_STATE); return VECTOR; }
  {VECTOR}                                    { return VECTOR; }

  {CHAR}/{ITERATOR_PATTERN}                   { yybeginstate(ITERATOR_STATE); return CHAR; }
  {CHAR}                                      { return CHAR; }
  {STRING}/{ITERATOR_PATTERN}                 { yybeginstate(ITERATOR_STATE); return STRING; }
  {STRING}/"_"                                { yybeginstate(DROP_CUT_STATE); return STRING; }
  {STRING}                                    { return STRING; }

  {VARIABLE_PATTERN}/{ITERATOR_PATTERN}       { yybeginstate(ITERATOR_STATE); return VARIABLE_PATTERN; }
  {VARIABLE_PATTERN}                          { return VARIABLE_PATTERN; }

  {OPERATOR}/{ITERATOR_PATTERN}               { yybeginstate(ITERATOR_STATE); return OPERATOR;}
  {OPERATOR}                                  { return OPERATOR;}
}

[^] { return BAD_CHARACTER; }
