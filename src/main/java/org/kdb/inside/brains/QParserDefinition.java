package org.kdb.inside.brains;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.parser.QParser;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QTypes;

public final class QParserDefinition implements ParserDefinition {
    public static final TokenSet COMMENTS = TokenSet.create(QTypes.LINE_COMMENT, QTypes.BLOCK_COMMENT);
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    public static final TokenSet STRING_LITERALS = TokenSet.create(QTypes.SYMBOL, QTypes.STRING);

    public static final IFileElementType FILE = new IFileElementType(Language.findInstance(QLanguage.class));

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return QLexer.newLexer();
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRING_LITERALS;
    }

    @Override
    public PsiParser createParser(final Project project) {
        return new QParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return QTypes.Factory.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new QFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
