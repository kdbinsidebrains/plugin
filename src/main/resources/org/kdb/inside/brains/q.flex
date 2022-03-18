package org.kdb.inside.brains;

import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
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
    private int parensCount = 0;
    private int bracesCount = 0;
    private boolean queryParsing = false;

    public QLexer() {
        this(null);
    }

    public void beginQuery() {
        queryParsing = true;
    }

    public void finishQuery() {
        queryParsing = false;
    }

    public void openParen() {
        if (queryParsing) {
            parensCount++;
        }
    }

    public void closeParen() {
        if (queryParsing) {
            parensCount--;
        }
    }

    public void openBrace() {
        if (queryParsing) {
            bracesCount++;
        }
    }

    public void closeBrace() {
        if (queryParsing) {
            bracesCount--;
        }
    }

    public boolean isQuerySplitter() {
        return queryParsing && parensCount == 0 && bracesCount == 0;
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
OperatorOthers=[!#@_\?\.\^\$]
Operator={OperatorEquality}|{OperatorOrder}|{OperatorArithmetic}|{OperatorWeight}|{OperatorOthers}

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

// (abs x) or abs[x]
UnaryFunction=(abs|all|any|asc|iasc|attr|avg|avgs|ceiling|count|
    cols|cos|acos|deltas|desc|idesc|dev|sdev|differ|distinct|
    enlist|eval|reval|exit|exp|first|last|fkeys|flip|floor|
    get|getenv|group|gtime|ltime|hcount|hdel|hopen|hclose|hsym|
    inv|key|keys|load|log|lower|upper|max|maxs|md5|med|meta|min|
    mins|neg|next|prev|not|null|parse|prd|prds|rand|rank|ratios|
    raze|read0|read1|reciprocal|reverse|save|rsave|show|signum|sin|
    asin|sqrt|string|sum|sums|system|tables|tan|atan|til|trim|ltrim|rtrim|
    type|value|var|svar|view|views|where|fills)

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
UnclosedString        = \"[^\"]*
String                = {UnclosedString}\"

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
%state ITERATOR_STATE
%state COMMAND_IMPORT_STATE
%state COMMAND_CONTEXT_STATE
%state COMMAND_SYSTEM_STATE
%state COMMENT_ALL_STATE
%state COMMENT_BLOCK_STATE
%state DROP_CUT_STATE
%state NEGATIVE_ATOM_STATE

%%

<COMMAND_IMPORT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {FilePath}                                 { return FILE_PATH_PATTERN; }
  {NewLine}                                { yybegin(YYINITIAL); return NEW_LINE; }
}

<COMMAND_CONTEXT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {Variable}                                 { return VARIABLE_PATTERN;}
  {NewLine}                                { yybegin(YYINITIAL); return NEW_LINE; }
}

<COMMAND_SYSTEM_STATE> {
  {NewLine}                                { yybegin(YYINITIAL); return NEW_LINE; }
  {WhiteSpace}+"/".*                         { yybegin(YYINITIAL); return LINE_COMMENT; }
  {CommandArguments}                         { return COMMAND_ARGUMENTS;}
}

<COMMENT_ALL_STATE> {
  .*{NewLine}?                             { return BLOCK_COMMENT; }
}

<COMMENT_BLOCK_STATE> {
  ^"\\"/{NewLine}+                         { yybegin(YYINITIAL); return BLOCK_COMMENT; }
  .*{NewLine}?                             { return BLOCK_COMMENT; }
}

<NEGATIVE_ATOM_STATE> {
  {Vector}                                    { yybegin(YYINITIAL); return VECTOR; }
  {NegativeAtom}                              { yybegin(YYINITIAL); return SIGNED_ATOM; }
}

<YYINITIAL> {
  "("{LineSpace}*")"                          { return VECTOR; }
  "("{LineSpace}*"::"{LineSpace}*")"          { return NILL; }

  "("                                         { openParen(); return PAREN_OPEN; }
  ")"                                         { closeParen(); return PAREN_CLOSE; }
  ";"                                         { return SEMICOLON; }
  "["                                         { return BRACKET_OPEN; }
  "]"                                         { return BRACKET_CLOSE; }
  "{"                                         { openBrace(); return BRACE_OPEN; }
  "}"                                         { closeBrace(); return BRACE_CLOSE; }
  ":"                                         { return COLON; }

  ","/{Iterator}                              { return ACCUMULATOR; }
  // Special case - the comma is a splitter if it's inside a query (not not inside a lambda that's inside the query)
  ","                                         { if(isQuerySplitter()) {return QUERY_SPLITTER; } else {return OPERATOR_COMMA;} }

  {ControlKeyword}/{WhiteSpace}*"["           { return CONTROL_KEYWORD; }
  {ConditionKeyword}/{WhiteSpace}*"["         { return CONDITION_KEYWORD; }

  {Iterator}/{NegativeAtom}                   { yybegin(NEGATIVE_ATOM_STATE); return ITERATOR; }
  {WhiteSpace}/{NegativeAtom}                 { yybegin(NEGATIVE_ATOM_STATE); return WHITE_SPACE; }
  {OperatorEquality}/{NegativeAtom}           { yybegin(NEGATIVE_ATOM_STATE); return OPERATOR_EQUALITY;}
  {OperatorOrder}/{NegativeAtom}              { yybegin(NEGATIVE_ATOM_STATE); return OPERATOR_ORDER;}
  {OperatorArithmetic}/{NegativeAtom}         { yybegin(NEGATIVE_ATOM_STATE); return OPERATOR_ARITHMETIC;}
  {OperatorWeight}/{NegativeAtom}             { yybegin(NEGATIVE_ATOM_STATE); return OPERATOR_WEIGHT;}
  {OperatorOthers}/{NegativeAtom}             { yybegin(NEGATIVE_ATOM_STATE); return OPERATOR_OTHERS;}

  {Operator}/{Iterator}                       { return ACCUMULATOR; }
  {Iterator}                                  { return ITERATOR; }

  {OperatorEquality}                          { return OPERATOR_EQUALITY;}
  {OperatorOrder}                             { return OPERATOR_ORDER;}
  {OperatorArithmetic}                        { return OPERATOR_ARITHMETIC;}
  {OperatorWeight}                            { return OPERATOR_WEIGHT;}
  {OperatorOthers}                            { return OPERATOR_OTHERS;}

  {WhiteSpace}+"/".*                          { return LINE_COMMENT; }
  {NewLine}+/{LineSpace}*"/"                { return WHITE_SPACE; }
  ^"/"/{NewLine}                            { yybegin(COMMENT_BLOCK_STATE); return BLOCK_COMMENT; }
  ^"\\"/{NewLine}                           { yybegin(COMMENT_ALL_STATE); return BLOCK_COMMENT; }
  ^"/".*                                      { if (zzCurrentPos == 0 || zzBuffer.length() == zzCurrentPos || zzBuffer.charAt(zzCurrentPos - 1) == '\n' || zzBuffer.charAt(zzCurrentPos - 1) == '\r') { return LINE_COMMENT;} return ITERATOR; }

  {NewLine}+                                { finishQuery(); return NEW_LINE; }

  ^"\\l"/{LineSpace}+!{NewLine}             { yybegin(COMMAND_IMPORT_STATE);  return COMMAND_IMPORT; }
  "system"/{WhiteSpace}*"\"l "                { return FUNCTION_IMPORT; }

  ^"\\d"/{LineSpace}+!{NewLine}             { yybegin(COMMAND_CONTEXT_STATE); return COMMAND_CONTEXT; }
  ^"\\d"/{NewLine}                          { return COMMAND_CONTEXT; }
  ^"\\d"/{WhiteSpace}                         { return COMMAND_CONTEXT; }

  ^{CommandName}/{LineSpace}+!{NewLine}     { yybegin(COMMAND_SYSTEM_STATE); return COMMAND_SYSTEM; }
  ^{CommandName}/{NewLine}                  { return COMMAND_SYSTEM; }
  ^{CommandName}/{WhiteSpace}                 { return COMMAND_SYSTEM; }

  ^{ModePrefix}                               { return MODE_PATTERN; }

  {TypeCast}                                  { return TYPE_CAST_PATTERN; }

  "-"[0-9]+"!"                                { return UNARY_FUNCTION; }
  [0-6]":"/{Iterator}                         { return BINARY_FUNCTION; }
  [0-6]":"/[^\[]                              { return BINARY_FUNCTION; }

  {Symbol}                                    { return SYMBOL_PATTERN; }
  {WhiteSpace}                                { return WHITE_SPACE; }

  {QueryType}                                 { beginQuery(); return QUERY_TYPE; }
  {QueryGroup}                                { return QUERY_BY; }
  {QueryFrom}                                 { finishQuery(); return QUERY_FROM; }

  {UnaryFunction}                             { return UNARY_FUNCTION; }
  {BinaryFunction}                            { return BINARY_FUNCTION; }
  {ComplexFunction}                           { return COMPLEX_FUNCTION; }

  {Vector}                                    { return VECTOR; }
  {NegativeAtom}                              { if (isNegativeSign()) { return SIGNED_ATOM; } else { yypushback(yylength() - 1); return OPERATOR_ARITHMETIC; } }
  {SignedAtom}                                { return SIGNED_ATOM; }
  {UnsignedAtom}                              { return UNSIGNED_ATOM; }
  {Char}                                      { return CHAR; }
  {Variable}                                  { return VARIABLE_PATTERN; }
  {String}                                    |
  {UnclosedString}                            { return STRING; }
}

[^] { return BAD_CHARACTER; }
