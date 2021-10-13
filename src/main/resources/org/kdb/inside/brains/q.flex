package org.kdb.inside.brains;

import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.kdb.inside.brains.psi.QTypes.*;

%%

%public %class QLexer
%extends org.kdb.inside.brains.psi.QLexerBase
%unicode
%function advance
%type IElementType
%eof{  return LINE_BREAK;
%eof}

%{
    public QLexer() {
        this(null);
    }

    public static com.intellij.lexer.Lexer newLexer() {
        return new com.intellij.lexer.FlexAdapter(new QLexer());
    }
%}

// Patterns
LineSpace=[\ \t\f]
LineBreak=\r\n|\r|\n|<<eof>>
WhiteSpace=({LineSpace}|{LineBreak})*{LineSpace}+

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
ConditionKeyword=":"|"?"|"$"|"@"|"." // ":" is from k3

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
    type|value|var|svar|view|views|where)

// and[x;y] or (x and y)
BinaryFunction=(and|xasc|asof|mavg|wavg|bin|binr|mcount|xcol|
    xcols|cor|cov|scov|cross|csv|xdesc|mdev|div|dsave|each|peach|ema|except|xexp|fby|set|setenv|
    ij|ijf|in|insert|inter|xkey|like|lj|ljf|xlog|lsq|mmax|mmin|mmu|mod|xprev|or|over|scan|pj|prior|
    rotate|ss|sublist|msum|wsum|sv|uj|ujf|union|ungroup|upsert|vs|within|xbar|xgroup|xrank)

// ej[c;t1;t2]
ComplexFunction=(aj|aj0|ajf|ajf0|ej|ssr|wj|wj1)

CommandName="\\"(\w+|[12\\])
CommandArguments=\w+

TypeCast=(("`"\w*)|("\""\w*"\""))"$"

IntCode=[ihjfepnuvt]
FloatCode=[fe]
TimeCode=[uvt]
DatetimeCode=[mdz]
TimestampCode=[pn]
GuidCode="g"

Null=0[Nn]
Infinity=-?0[iIwW]

ByteLetter=[[:digit:]A-Fa-f]
Byte="0x"{ByteLetter}{ByteLetter}?
ByteList=("0x"{ByteLetter}{2}{ByteLetter}+)

Boolean=[01]"b"
BooleanList=([01][01]+"b")

IntegerAtom=(0|[1-9][0-9]*)
//INTEGER=-?(0|[1-9][0-9]*)
Integer=-?({IntegerAtom}|{Null}|{Infinity})
IntegerList=({Integer}({WhiteSpace}{Integer})+{IntCode}?)

FloatDigAtom={IntegerAtom}(\.[0-9]*)|(\.[0-9]+)
FloatExpAtom={IntegerAtom}(\.[0-9]+)?([eE][+-]?[0-9]*)
Float=-?({IntegerAtom}|{FloatDigAtom}|{FloatExpAtom}|{Null}|{Infinity})
FloatList=({Float}({WhiteSpace}{Float})+{FloatCode}?)

MonthAtom=[:digit:]{4}\.[:digit:]{2}
//MONTH=-?[:digit:]{4}\.[:digit:]{2}
Month=-?({MonthAtom}|{Null}|{Infinity})
MonthList=({Month}({WhiteSpace}{Month})+"m"?)

TimeAtom=[:digit:]+(\:[:digit:]{2}(\:[0-5][:digit:](\.[:digit:]+)?)?)?
//TIME=-?[:digit:]+(\:[:digit:]{2}(\:[0-5][:digit:](\.[:digit:]+)?)?)?
Time=-?({IntegerAtom}|{Null}|{MinuteAtom}|{SecondAtom}|{TimeAtom}|{Infinity})
TimeList=({Time}({WhiteSpace}{Time})+{TimeCode}?)

DateAtom={MonthAtom}\.[:digit:]{2}
Date={DateAtom}|{Null}|{Infinity}
DateList=({Date}({WhiteSpace}{Date})+"d"?)

DatetimeAtom={DateAtom}"T"{TimeAtom}
Datetime=-?({DatetimeAtom}|{DateAtom}|{Null}|{Infinity})
DatetimeList=({Datetime}({WhiteSpace}{Datetime})+"z"?)

//TIMESTAMP=-?{DATE}("D"{TIME})?
TimestampAtom={DateAtom}("D"{TimeAtom})?
TimespanAtom={IntegerAtom}"D"({TimeAtom})?
Timestamp=-?({TimestampAtom}|{TimespanAtom}|{FloatDigAtom}|{Time})
TimestampList=({Timestamp}({WhiteSpace}{Timestamp})+{TimestampCode}?)

MinuteAtom=[:digit:]{2,3}\:[0-5][:digit:]
//MINUTE=-?[:digit:]{2,3}\:[0-5][:digit:]
Minute=-?{MinuteAtom}
MinuteList={Minute}({WhiteSpace}{Minute})+

//SECOND=-?{MINUTE}\:[0-5][:digit:]
SecondAtom={MinuteAtom}\:[0-5][:digit:]
Second=-?{SecondAtom}
SecondList=({Second}({WhiteSpace}{Second})+)

CharAtom=([^\\\"]|\\[^\ \t])
Char=\"{CharAtom}\"
UnclosedString        = \"[^\"]*
String                = {UnclosedString}\"
//String=(\"\"|\"{CharAtom}{CharAtom}+\")

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

Vactor={BooleanList}|{ByteList}|{IntegerList}|{FloatList}|
    {TimestampList}|{TimeList}|{MonthList}|{DateList}|{DatetimeList}|{MinuteList}|{SecondList}

%state MODE_STATE
%state ITERATOR_STATE
%state COMMAND_IMPORT_STATE
%state COMMAND_CONTEXT_STATE
%state COMMAND_SYSTEM_STATE
%state COMMENT_ALL_STATE
%state COMMENT_BLOCK_STATE
%state DROP_CUT_STATE

%%

<COMMAND_IMPORT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {FilePath}                                 { return FILE_PATH_PATTERN; }
  {LineBreak}                                { yybegin(YYINITIAL); return LINE_BREAK; }
}

<COMMAND_CONTEXT_STATE> {
  {LineSpace}+                               { return WHITE_SPACE; }
  {Variable}                          { return VARIABLE_PATTERN;}
  {LineBreak}                                { yybegin(YYINITIAL); return LINE_BREAK; }
}

<COMMAND_SYSTEM_STATE> {
  {LineBreak}                                { yybegin(YYINITIAL); return LINE_BREAK; }
  {WhiteSpace}+"/".*                         { yybegin(YYINITIAL); return LINE_COMMENT; }
  {WhiteSpace}                               { return WHITE_SPACE; }
  {CommandArguments}                      { return COMMAND_ARGUMENTS;}
}

<COMMENT_ALL_STATE> {
  .*{LineBreak}?                             { return BLOCK_COMMENT; }
}

<COMMENT_BLOCK_STATE> {
  ^"\\"/{LineBreak}+                        { yybegin(YYINITIAL); return BLOCK_COMMENT; }
  .*{LineBreak}?                            { return BLOCK_COMMENT; }
}

<YYINITIAL> {
  "("{LineSpace}*")"                         { return VECTOR; }
  "("{LineSpace}*"::"{LineSpace}*")"        { return NILL; }

  "("                                         { return PAREN_OPEN; }
  ")"                                         { return PAREN_CLOSE; }
  ";"                                         { return SEMICOLON; }
  "["                                         { return BRACKET_OPEN; }
  "]"                                         { return BRACKET_CLOSE; }
  "{"                                         { beginLambda(); return BRACE_OPEN; }
  "}"                                         { finishLambda(); return BRACE_CLOSE; }
  ":"                                         { return COLON; }

  ","/{Iterator}                              { return ACCUMULATOR; }
  // Special case - the comma is a splitter if it's inside a query (not not inside a lambda that's inside the query)
  ","                                         { if(isQuerySplitter()) {return QUERY_SPLITTER; } else {return OPERATOR_COMMA;} }

  {ControlKeyword}/{WhiteSpace}*"["         { return CONTROL_KEYWORD; }
  {ConditionKeyword}/{WhiteSpace}*"["       { return CONDITION_KEYWORD; }

  {Operator}/{Iterator}                       { return ACCUMULATOR; }
  {Iterator}                                  { return ITERATOR; }

  {OperatorEquality}                         { return OPERATOR_EQUALITY;}
  {OperatorOrder}                            { return OPERATOR_ORDER;}
  {OperatorArithmetic}                       { return OPERATOR_ARITHMETIC;}
  {OperatorWeight}                           { return OPERATOR_WEIGHT;}
  {OperatorOthers}                           { return OPERATOR_OTHERS;}

  {WhiteSpace}+"/".*                         { return LINE_COMMENT; }
  {LineBreak}+/{LineSpace}*"/"              { return WHITE_SPACE; }
  ^"/"/{LineBreak}                           { yybegin(COMMENT_BLOCK_STATE); return BLOCK_COMMENT; }
  ^"\\"/{LineBreak}                          { yybegin(COMMENT_ALL_STATE); return BLOCK_COMMENT; }
  ^"/".*                                      { if (zzCurrentPos == 0 || zzBuffer.length() == zzCurrentPos || zzBuffer.charAt(zzCurrentPos - 1) == '\n' || zzBuffer.charAt(zzCurrentPos - 1) == '\r') { return LINE_COMMENT;} return ITERATOR; }

  {LineBreak}+                               { finishQuery(); return LINE_BREAK; }

  ^"\\l"/{LineSpace}+!{LineBreak}           { yybegin(COMMAND_IMPORT_STATE);  return COMMAND_IMPORT; }
  "system"/{WhiteSpace}*"\"l "               { return FUNCTION_IMPORT; }

  ^"\\d"/{LineSpace}+!{LineBreak}           { yybegin(COMMAND_CONTEXT_STATE); return COMMAND_CONTEXT; }
  ^"\\d"/{LineBreak}                         { return COMMAND_CONTEXT; }
  ^"\\d"/{WhiteSpace}                        { return COMMAND_CONTEXT; }

  ^{CommandName}/{LineSpace}+!{LineBreak} { yybegin(COMMAND_SYSTEM_STATE); return COMMAND_SYSTEM; }
  ^{CommandName}/{LineBreak}               { return COMMAND_SYSTEM; }
  ^{CommandName}/{WhiteSpace}              { return COMMAND_SYSTEM; }

  ^{ModePrefix}                             { return MODE_PATTERN; }

  {TypeCast}                         { return TYPE_CAST_PATTERN; }

  "-"[0-9]+"!"                                { return UNARY_FUNCTION; }
  [0-6]":"/{Iterator}                         { return BINARY_FUNCTION; }
  [0-6]":"/[^\[]                              { return BINARY_FUNCTION; }

  {Symbol}                                   { return SYMBOL_PATTERN; }
  {WhiteSpace}                               { return WHITE_SPACE; }

  {QueryType}                                { beginQuery(); return QUERY_TYPE; }
  {QueryGroup}                                  { return QUERY_BY; }
  {QueryFrom}                                { finishQuery(); return QUERY_FROM; }

  {UnaryFunction}                            { return UNARY_FUNCTION; }
  {BinaryFunction}                           { return BINARY_FUNCTION; }
  {ComplexFunction}                          { return COMPLEX_FUNCTION; }

  {SignedAtom}                               { return SIGNED_ATOM; }
  {UnsignedAtom}                             { return UNSIGNED_ATOM; }
  {Vactor}                                    { return VECTOR; }
  {Char}                                { return CHAR; }
  {Variable}                            { return VARIABLE_PATTERN; }
  {String}                              |
  {UnclosedString}                      { return STRING; }
}

[^] { return BAD_CHARACTER; }
