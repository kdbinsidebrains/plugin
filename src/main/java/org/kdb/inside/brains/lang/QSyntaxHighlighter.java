package org.kdb.inside.brains.lang;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLexer;
import org.kdb.inside.brains.psi.QTypes;

import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class QSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey NILL = createTextAttributesKey("Q_NILL",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ITERATOR = createTextAttributesKey("Q_ITERATOR",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey VARIABLE = createTextAttributesKey("Q_VARIABLE",
            DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey COMMAND = createTextAttributesKey("Q_COMMAND",
            DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey CONTEXT = createTextAttributesKey("Q_CONTEXT",
            DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey TYPECAST = createTextAttributesKey("Q_TYPECASE",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey OPERATOR = createTextAttributesKey("Q_OPERATOR",
            DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey SYMBOL = createTextAttributesKey("Q_SYMBOL",
            DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey ATOM = createTextAttributesKey("Q_ATOM",
            DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey ATOMS = createTextAttributesKey("Q_ATOMS",
            DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey IMPORT = createTextAttributesKey("Q_IMPORT",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey KEYWORD = createTextAttributesKey("Q_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey QUERY = createTextAttributesKey("Q_QUERY",
            DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey CHAR = createTextAttributesKey("Q_CHAR",
            DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey STRING = createTextAttributesKey("Q_STRING",
            DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey BRACES = createTextAttributesKey("Q_BRACES",
            DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACKETS = createTextAttributesKey("Q_BRACKETS",
            DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey PARENTHESES = createTextAttributesKey("Q_PARENTHESES",
            DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey COMMENT = createTextAttributesKey("Q_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("Q_BAD_CHARACTER",
            DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);

    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    private static Map.Entry<IElementType, TextAttributesKey[]> entry(IElementType type, TextAttributesKey attr) {
        return Map.entry(type, new TextAttributesKey[]{attr});
    }

    private static final Map<IElementType, TextAttributesKey[]> MAPPING = Map.ofEntries(
            entry(QTypes.MODE_PATTERN, COMMAND),
            entry(QTypes.ITERATOR, ITERATOR),
            entry(QTypes.SYMBOL_PATTERN, SYMBOL),
            entry(QTypes.OPERATOR, OPERATOR),
            entry(QTypes.TYPE_CAST_PATTERN, TYPECAST),

            entry(QTypes.COMMAND_SYSTEM, COMMAND),
            entry(QTypes.COMMAND_ARGUMENTS, COMMAND),

            entry(QTypes.COMMAND_IMPORT, IMPORT),

            entry(QTypes.COMMAND_CONTEXT, CONTEXT),

            entry(QTypes.VARIABLE_PATTERN, VARIABLE),

            entry(QTypes.QUERY_BY, QUERY),
            entry(QTypes.QUERY_TYPE, QUERY),
            entry(QTypes.QUERY_FROM, QUERY),

            entry(QTypes.COLON, KEYWORD),
            entry(QTypes.FUNCTION, KEYWORD),
            entry(QTypes.CONTROL_PATTERN, KEYWORD),
            entry(QTypes.CONDITION_PATTERN, KEYWORD),

            entry(QTypes.BRACE_OPEN, BRACES),
            entry(QTypes.BRACE_CLOSE, BRACES),

            entry(QTypes.BRACKET_OPEN, BRACKETS),
            entry(QTypes.BRACKET_CLOSE, BRACKETS),

            entry(QTypes.PAREN_OPEN, PARENTHESES),
            entry(QTypes.PAREN_CLOSE, PARENTHESES),

            entry(QTypes.COMMENT, COMMENT),

            entry(QTypes.NILL, NILL),
            entry(QTypes.ATOM, ATOM),
            entry(QTypes.ATOMS, ATOMS),
            entry(QTypes.CHAR, CHAR),
            entry(QTypes.STRING, STRING),

            entry(TokenType.BAD_CHARACTER, BAD_CHARACTER)
    );

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new FlexAdapter(new QLexer(null));
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        return MAPPING.getOrDefault(tokenType, EMPTY_KEYS);
    }
}