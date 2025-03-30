package org.kdb.inside.brains;

import com.intellij.psi.tree.IElementType;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.kdb.inside.brains.psi.QTypes.*;

%%

%public %class QLexer
%implements com.intellij.lexer.FlexLexer
%unicode
%function advance
%type IElementType

%eof{
    yyresetstate();
%eof}

%{
    private final Deque<Integer> states = new ArrayDeque<>(10);

    private QLexer() {
        this(null);
    }

    private void yypushstate(int state) {
        states.push(yystate());
        yybegin(state);
    }

    private void yypopstate() {
        yybegin(states.isEmpty() ? YYINITIAL : states.pop());
    }

    private void yyresetstate() {
        states.clear();
        yybegin(YYINITIAL);
    }

    public boolean isNegativeSign() {
        if (getTokenStart() == 0) {
            return true;
        }
        final char ch = yycharat(-1);
        // Any whitespace before - it's negative sing. If's also negative if it goes aftyer any brakes
        return Character.isWhitespace(ch) || ch == '[' || ch == '(' || ch == '{';
    }

    public static com.intellij.lexer.Lexer newLexer() {
        return new com.intellij.lexer.FlexAdapter(new QLexer());
    }
%}

// Patterns
NewLine=\r\n|\r|\n|<<eof>>
LineSpace=[\ \t\f]
WhiteSpace=({LineSpace}|{NewLine})*{LineSpace}+

// Changing anything - don't forget update QLanguage
Iterator=("/": | \\: | ': | "/" | \\ | ' )

OperatorEquality=(\~ | = | <>)
OperatorOrder=(<= | >= | < | >)
OperatorArithmetic=[\+\-\*%]
OperatorWeight=[&\|]
OperatorCut=[_]
OperatorApply=[\.]
OperatorOthers=[!#@\?\^\$]

ModePrefix=[a-zA-Z]")"
Variable=[\.a-zA-Z][\._a-zA-Z0-9]*
FilePath=[^|?*<\">+\[\]'\n\r\ \t\f]+

// Keywords and system commands
// Changing anything - don't forget update QLanguage
ControlKeyword=(if|do|while)
ConditionKeyword=":"|"?"|"$"|"@"|"."|"!" // ":" is from k3

// QSQL supporting
QueryType=(select|exec|update|delete)
QueryGroup=(by)
QueryFrom=(from)

// Special case
Where=(where)

// (abs x) or abs[x]
UnaryFunction=(abs|all|any|asc|iasc|attr|avg|avgs|ceiling|count|
    cols|cos|acos|deltas|desc|idesc|dev|sdev|differ|distinct|
    enlist|eval|reval|exit|exp|first|last|fkeys|flip|floor|
    get|getenv|group|gtime|ltime|hcount|hdel|hopen|hclose|hsym|
    inv|key|keys|load|log|lower|upper|max|maxs|md5|med|meta|min|
    mins|neg|next|prev|not|null|parse|prd|prds|rand|rank|ratios|
    raze|read0|read1|2:|reciprocal|reverse|save|rsave|show|signum|sin|
    asin|sqrt|string|sum|sums|system|tables|tan|atan|til|trim|ltrim|rtrim|
    type|value|var|svar|view|views|fills)

UnaryPrimitive="#:"|"?:"|"*:"|"+:"|"_:"|".:"|"=:"|"%:"|"~:"|">:"|"!:"|"!:"|"|\\"|"&\\"|"-:"|"~:"|"^:"|"*\\"|",/"|"%:"|"|:"|"$:"|"+\\"|"@:"|".:"|"1::"|"2::"

// and[x;y] or (x and y)
BinaryFunction=(and|xasc|asof|mavg|wavg|bin|binr|mcount|xcol|
    xcols|cor|cov|scov|cross|csv|xdesc|mdev|div|dsave|each|peach|ema|except|xexp|fby|set|setenv|
    ij|ijf|in|insert|inter|xkey|like|lj|ljf|xlog|lsq|mmax|mmin|mmu|mod|xprev|or|over|scan|pj|prior|
    rotate|ss|sublist|msum|wsum|sv|uj|ujf|union|ungroup|upsert|vs|within|xbar|xgroup|xrank)

// ej[c;t1;t2]
ComplexFunction=(aj|aj0|ajf|ajf0|ej|ssr|wj|wj1)

CommandName="\\"(\w+|[12\\])
CommandArguments=[^\r\n]+

TypeCast=(("`"\w*)|("\""\w*"\""))"$"

IntCode=[ihjfepnuvt]
FloatCode=[fe]
TimeCode=[uvt]
DatetimeCode=[mdz]
TimestampCode=[pn]
GuidCode="g"

Null=0[Nn]
Infinity=0[iIwW]

ByteLetter=[[:digit:]A-Fa-f]
Byte="0x"{ByteLetter}{ByteLetter}?
ByteList=("0x"{ByteLetter}{2}{ByteLetter}+)

Boolean=[01]"b"
BooleanList=([01][01]+"b")

IntegerAtom=(0|([1-9][0-9]*))
Integer=-?({IntegerAtom}|{Null}|{Infinity})
IntegerList={Integer}({WhiteSpace}{Integer})+{IntCode}?

FloatDigAtom={IntegerAtom}(\.[0-9]*)|(\.[0-9]+)
FloatExpAtom={IntegerAtom}(\.[0-9]+)?([eE][+-]?[0-9]*)
Float=-?({IntegerAtom}|{FloatDigAtom}|{FloatExpAtom}|{Null}|{Infinity})
FloatList={Float}({WhiteSpace}{Float})+{FloatCode}?

MonthAtom=[:digit:]{4}\.[:digit:]{2}
Month=-?({MonthAtom}|{Null}|{Infinity})
MonthList={Month}({WhiteSpace}{Month})+"m"?

TimeAtom=[:digit:]+(\:[:digit:]{2}(\:[0-5][:digit:](\.[:digit:]+)?)?)?
Time=-?({IntegerAtom}|{Null}|{MinuteAtom}|{SecondAtom}|{TimeAtom}|{Infinity})
TimeList={Time}({WhiteSpace}{Time})+{TimeCode}?

DateAtom={MonthAtom}\.[:digit:]{2}
Date=-?{DateAtom}|{Null}|{Infinity}
DateList={Date}({WhiteSpace}{Date})+"d"?

DatetimeAtom={DateAtom}"T"{TimeAtom}
Datetime=-?({DatetimeAtom}|{DateAtom}|{Null}|{Infinity})
DatetimeList={Datetime}({WhiteSpace}{Datetime})+"z"?

TimestampAtom={DateAtom}("D"{TimeAtom})?
TimespanAtom={IntegerAtom}"D"({TimeAtom})?
Timestamp=-?({TimestampAtom}|{TimespanAtom}|{IntegerAtom}|{Null}|{MinuteAtom}|{SecondAtom}|{TimeAtom}|{FloatDigAtom}|{Infinity})
TimestampList={Timestamp}({WhiteSpace}{Timestamp})+{TimestampCode}?

MinuteAtom=[:digit:]{2,3}\:[0-5][:digit:]
Minute=-?{MinuteAtom}
MinuteList={Minute}({WhiteSpace}{Minute})+

SecondAtom={MinuteAtom}\:[0-5][:digit:]
Second=-?{SecondAtom}
SecondList={Second}({WhiteSpace}{Second})+

CharAtom=([^\\\"]|\\[^\ \t])
Char=\"{CharAtom}\"
UnclosedString        = \"{CharAtom}+
String                = (\"\")|({UnclosedString}\")

Symbol="`"[.:/_a-zA-Z0-9]*

SignedAtom=
    {IntegerAtom}{IntCode}?|
    ({FloatDigAtom}|{FloatExpAtom}){FloatCode}?|
    {DatetimeAtom}"z"?|
    {MonthAtom}"m"?|
    ({TimestampAtom}|{TimespanAtom}|{FloatDigAtom}){TimestampCode}?|
    ({MinuteAtom}|{SecondAtom}|{TimeAtom}){TimeCode}?|
    ({Infinity}|{Null})({IntCode}|{DatetimeCode})?

UnsignedAtom={Boolean}|{Byte}|{Null}{GuidCode}|{DateAtom}[dz]?

NegativeAtom="-"{SignedAtom}

Vector={BooleanList}|{ByteList}|{IntegerList}|{FloatList}|
    {TimestampList}|{TimeList}|{MonthList}|{DateList}|{DatetimeList}|{MinuteList}|{SecondList}

%state MODE_STATE
%state QUERY_COLUMNS_STATE
%state QUERY_SOURCE_STATE
%state ITERATOR_STATE
%state COMMAND_IMPORT_STATE
%state COMMAND_CONTEXT_STATE
%state COMMAND_SYSTEM_STATE
%state COMMAND_SYSTEM_ARGUMENTS_STATE
%state COMMENT_ALL_STATE
%state COMMENT_BLOCK_STATE
%state DROP_CUT_STATE
%state NEGATIVE_ATOM_STATE
%state LINE_COMMENT_STATE

%%

<COMMAND_IMPORT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {FilePath}                                 { return FILE_PATH_PATTERN; }
  {NewLine}                                  { yyresetstate(); return NEW_LINE; }
}

<COMMAND_CONTEXT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {Variable}                                 { return VARIABLE_PATTERN;}
  {NewLine}                                  { yyresetstate(); return NEW_LINE; }
}

<COMMAND_SYSTEM_STATE> {
  {LineSpace}+                               { yybegin(COMMAND_SYSTEM_ARGUMENTS_STATE); return WHITE_SPACE; }
}

<COMMAND_SYSTEM_ARGUMENTS_STATE> {
  {NewLine}                                  { yyresetstate(); return NEW_LINE; }
  {WhiteSpace}+/"/".*                        { yybegin(LINE_COMMENT_STATE); return WHITE_SPACE; }
  {CommandArguments}                         { return COMMAND_ARGUMENTS;}
}

<COMMENT_ALL_STATE> {
  .*{NewLine}?                               { return BLOCK_COMMENT; }
}

<COMMENT_BLOCK_STATE> {
  ^"\\"/{NewLine}+                           { yybegin(YYINITIAL); return BLOCK_COMMENT; }
  .*{NewLine}?                               { return BLOCK_COMMENT; }
}

<NEGATIVE_ATOM_STATE> {
  {Vector}                                   { yypopstate(); return VECTOR; }
  {NegativeAtom}                             { yypopstate(); return SIGNED_ATOM; }
}

<QUERY_COLUMNS_STATE> {
  ","                                        { return QUERY_SPLITTER; }
}

<QUERY_SOURCE_STATE> {
  {Where}                                    { yybegin(YYINITIAL); return QUERY_WHERE; }
}

<LINE_COMMENT_STATE> {
    "/".*                                    { yybegin(YYINITIAL); return LINE_COMMENT; }
}

<YYINITIAL, QUERY_COLUMNS_STATE, QUERY_SOURCE_STATE> {
//  "("{LineSpace}*")"                         { return VECTOR; }
  "("{LineSpace}*"::"{LineSpace}*")"         { return NILL; }

  "("                                        { yypushstate(YYINITIAL); return PAREN_OPEN; }
  ")"                                        { yypopstate(); return PAREN_CLOSE; }
  "["                                        { yypushstate(YYINITIAL); return BRACKET_OPEN; }
  "]"                                        { yypopstate(); return BRACKET_CLOSE; }
  "{"                                        { yypushstate(YYINITIAL); return BRACE_OPEN; }
  "}"                                        { yypopstate(); return BRACE_CLOSE; }

  ":"                                        { return COLON; }
  ";"                                        { yybegin(YYINITIAL); return SEMICOLON; }

  ","                                        { return OPERATOR_COMMA; }
  ","/{Iterator}                             { return OPERATOR_COMMA; }

  {ControlKeyword}/{WhiteSpace}*"["          { return CONTROL_KEYWORD; }
  {ConditionKeyword}/{WhiteSpace}*"["        { return CONDITION_KEYWORD; }

  {Iterator}/{NegativeAtom}                  { yypushstate(NEGATIVE_ATOM_STATE); return ITERATOR; }
  {WhiteSpace}/{NegativeAtom}                { yypushstate(NEGATIVE_ATOM_STATE); return WHITE_SPACE; }
  {OperatorCut}/{NegativeAtom}               { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_CUT;}
  {OperatorApply}/{NegativeAtom}             { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_APPLY;}
  {OperatorEquality}/{NegativeAtom}          { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_EQUALITY;}
  {OperatorOrder}/{NegativeAtom}             { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_ORDER;}
  {OperatorArithmetic}/{NegativeAtom}        { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_ARITHMETIC;}
  {OperatorWeight}/{NegativeAtom}            { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_WEIGHT;}
  {OperatorOthers}/{NegativeAtom}            { yypushstate(NEGATIVE_ATOM_STATE); return OPERATOR_OTHERS;}

  {Iterator}                                 { return ITERATOR; }

  {OperatorCut}                              { return OPERATOR_CUT;}
  {OperatorApply}                            { return OPERATOR_APPLY;}
  {OperatorEquality}                         { return OPERATOR_EQUALITY;}
  {OperatorOrder}                            { return OPERATOR_ORDER;}
  {OperatorArithmetic}                       { return OPERATOR_ARITHMETIC;}
  {OperatorWeight}                           { return OPERATOR_WEIGHT;}
  {OperatorOthers}                           { return OPERATOR_OTHERS;}

  {WhiteSpace}+/"/".*                        { yybegin(LINE_COMMENT_STATE); return WHITE_SPACE; }
  {NewLine}+/{LineSpace}*"/"                 { return WHITE_SPACE; }
  ^"/"/{NewLine}                             { yybegin(COMMENT_BLOCK_STATE); return BLOCK_COMMENT; }
  ^"\\"/{NewLine}                            { yybegin(COMMENT_ALL_STATE); return BLOCK_COMMENT; }
  ^"/".*                                     { if (zzCurrentPos == 0 || zzBuffer.length() == zzCurrentPos || zzBuffer.charAt(zzCurrentPos - 1) == '\n' || zzBuffer.charAt(zzCurrentPos - 1) == '\r') { return LINE_COMMENT;} return ITERATOR; }

  {NewLine}+                                 { yyresetstate(); return NEW_LINE; }

  ^"\\l"/{LineSpace}+!{NewLine}              { yybegin(COMMAND_IMPORT_STATE); return COMMAND_IMPORT; }
  "system"/{WhiteSpace}*"\"l "               { return FUNCTION_IMPORT; }

  ^"\\d"/{LineSpace}+!{NewLine}              { yybegin(COMMAND_CONTEXT_STATE); return COMMAND_CONTEXT; }
  ^"\\d"/{NewLine}                           { return COMMAND_CONTEXT; }
  ^"\\d"/{WhiteSpace}                        { return COMMAND_CONTEXT; }

  ^{CommandName}/{LineSpace}+!{NewLine}      { yybegin(COMMAND_SYSTEM_STATE); return COMMAND_SYSTEM; }
  ^{CommandName}/{NewLine}                   { return COMMAND_SYSTEM; }
  ^{CommandName}/{WhiteSpace}                { return COMMAND_SYSTEM; }

  ^{ModePrefix}                              { if (zzCurrentPos == 0 || zzBuffer.charAt(zzCurrentPos - 1) == '\n') {return MODE_PATTERN; } else {yypushback(1); return VARIABLE_PATTERN;} }

  {TypeCast}                                 { return TYPE_CAST_PATTERN; }

  [0-6]":"/{Iterator}                        { return BINARY_FUNCTION; }
  [0-6]":"/[^\[]                             { return BINARY_FUNCTION; }
  // https://code.kx.com/q/basics/internal/
  "-"[0-9]+"!"                               { return INTERNAL_FUNCTION; }

  {Symbol}                                   { return SYMBOL_PATTERN; }
  {WhiteSpace}                               { return WHITE_SPACE; }

  {QueryType}                                { yybegin(QUERY_COLUMNS_STATE); return QUERY_TYPE; }
  {QueryGroup}                               { yybegin(QUERY_COLUMNS_STATE); return QUERY_BY; }
  {QueryFrom}                                { yybegin(QUERY_SOURCE_STATE); return QUERY_FROM; }

  {Where}                                    { return UNARY_FUNCTION; }
  {UnaryFunction}                            { return UNARY_FUNCTION; }
  {UnaryPrimitive}                           { return UNARY_FUNCTION; }
  {BinaryFunction}                           { return BINARY_FUNCTION; }
  {ComplexFunction}                          { return COMPLEX_FUNCTION; }

  {Vector}                                   { return VECTOR; }
  {NegativeAtom}                             { if (isNegativeSign()) { return SIGNED_ATOM; } else { yypushback(yylength() - 1); return OPERATOR_ARITHMETIC; } }
  {SignedAtom}                               { return SIGNED_ATOM; }
  {UnsignedAtom}                             { return UNSIGNED_ATOM; }
  {Char}                                     { return CHAR; }
  {Variable}                                 { return VARIABLE_PATTERN; }
  {String}                                   |
  {UnclosedString}                           { return STRING; }
}

[^] { return BAD_CHARACTER; }
