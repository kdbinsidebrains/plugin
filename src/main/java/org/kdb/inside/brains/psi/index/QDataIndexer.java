package org.kdb.inside.brains.psi.index;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.PsiDependentFileContent;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QDataIndexer implements DataIndexer<String, List<IdentifierDescriptor>, FileContent> {
    protected static final int VERSION = 17;

    private static final Logger log = Logger.getInstance(QDataIndexer.class);

    private static final TokenSet CONTEXT_SCOPE = TokenSet.create(CONTEXT);
    private static final TokenSet LOCAL_VARIABLE_SCOPE = TokenSet.create(LAMBDA_EXPR, TABLE_EXPR, DICT_EXPR);

    @Override
    public @NotNull Map<String, List<IdentifierDescriptor>> map(@NotNull FileContent content) {
        final long startedNanos = System.nanoTime();
        final String fileName = content.getFileName();

        log.info("Start indexing " + content.getFile() + ": " + fileName);

        final Map<String, List<IdentifierDescriptor>> res = new HashMap<>();

        final CharSequence text = content.getContentAsText();
        final int[] offsets = findAllOffsets(text);

        log.info(fileName + ": length - " + text.length() + ",  offsets - " + offsets.length);
        if (offsets.length == 0) {
            return Collections.emptyMap();
        }

        final LighterAST tree = ((PsiDependentFileContent) content).getLighterAST();

        final AtomicInteger index = new AtomicInteger();

        LightTreeUtil.processLeavesAtOffsets(offsets, tree, (tokenNode, offset) -> {
            log.info(fileName + ": processing offset " + index.incrementAndGet() + " of " + offsets.length + " - " + offset);
            final LighterASTNode node = tree.getParent(tokenNode);
            if (node == null) {
                return;
            }

            List<IndexEntry> indexes = null;
            final IElementType tokenType = node.getTokenType();
            if (tokenType == SYMBOL) {
                indexes = wrap(processSymbol(node, text));
            } else if (tokenType == TYPE_ASSIGNMENT_TYPE || tokenType == VAR_ASSIGNMENT_TYPE || tokenType == VAR_ACCUMULATOR_TYPE) {
                indexes = processAssignment(tree, node, text, offset);
            }

            if (indexes != null) {
                for (IndexEntry entry : indexes) {
                    log.info(fileName + ": index indexes generated - " + entry);
                    res.computeIfAbsent(entry.name, i -> new ArrayList<>()).add(new IdentifierDescriptor(entry.type, entry.range));
                }
            }
        });
        final long finishedNanos = System.nanoTime();
        log.info("Indexing finished with " + res.size() + " keywords at " + (finishedNanos - startedNanos) + "ns");
        return res;
    }

    private @Nullable IndexEntry processSymbol(LighterASTNode node, CharSequence text) {
        final TextRange range = new TextRange(node.getStartOffset() + 1, node.getEndOffset());
        String symbolValue = String.valueOf(range.subSequence(text));

        // We ignore root namespace
        if (symbolValue.isEmpty() || symbolValue.equals(".")) {
            return null;
        }

        // We ignore
        if (symbolValue.startsWith(":")) {
            return null;
        }

        // If it's set - a variable definition
        if (containsSetForward(text, range.getEndOffset() + 1) || containsSetBackward(text, range.getStartOffset() - 1)) {
            return new IndexEntry(symbolValue, IdentifierType.VARIABLE, range);
        }
        return new IndexEntry(symbolValue, IdentifierType.SYMBOL, range);
    }

    private @Nullable List<IndexEntry> processAssignment(LighterAST tree, LighterASTNode assignmentType, CharSequence text, Integer offset) {
        final LighterASTNode assignmentExpr = tree.getParent(assignmentType);
        if (assignmentExpr == null) {
            return null;
        }

        final List<LighterASTNode> assignmentItems = tree.getChildren(assignmentExpr);
        if (assignmentItems.size() < 3) {
            return null;
        }

        final LighterASTNode varDeclaration = assignmentItems.get(0);
        final IElementType varType = varDeclaration.getTokenType();
        if (varType != VAR_DECLARATION && varType != VAR_INDEXING && varType != PATTERN_DECLARATION) {
            return null;
        }

        // Ignore not global
        if (isLocal(tree, varDeclaration, offset, text)) {
            return null;
        }

        if (varType == VAR_INDEXING) {
            return wrap(processIndexingAssignment(tree, varDeclaration, text));
        }

        final LighterASTNode expression = assignmentItems.get(assignmentItems.size() - 1);
        if (varType == PATTERN_DECLARATION) {
            return processPatternAssignment(tree, varDeclaration, expression, text);
        }
        return wrap(createEntry(tree, varDeclaration, expression, text));
    }

    /**
     * <pre>
     *     QAssignmentExprImpl(ASSIGNMENT_EXPR)
     *          QPatternDeclarationImpl(PATTERN_DECLARATION)
     *              QTypedVariableImpl(TYPED_VARIABLE)
     *                  QVarDeclarationImpl(variableName65756) - variable name
     *                  PsiElement(:)
     *                  QLiteralExprImpl(LITERAL_EXPR)
     *              QTypedVariableImpl(TYPED_VARIABLE) - variable name
     *                  QVarDeclarationImpl(variableName45645)
     *                  PsiElement(:)
     *                  QVarReferenceImpl(testVar)
     *              QTypedVariableImpl(TYPED_VARIABLE) - variable name
     *                  QVarDeclarationImpl(variableName7768768)
     *                  PsiElement(:)
     *                  QLambdaExprImpl(LAMBDA_EXPR)
     *      QVarAssignmentTypeImpl(VAR_ASSIGNMENT_TYPE)
     *      QParenthesesExprImpl(PARENTHESES_EXPR)
     *          QLiteralExprImpl(LITERAL_EXPR)
     *          QLiteralExprImpl(LITERAL_EXPR)
     *          QLiteralExprImpl(LITERAL_EXPR)
     * </pre>
     */
    private List<IndexEntry> processPatternAssignment(LighterAST tree, LighterASTNode patternDeclaration, LighterASTNode patternValues, CharSequence text) {
        final List<LighterASTNode> variables = parensBySemicolon(tree, patternDeclaration);
        final List<LighterASTNode> values = patternValues.getTokenType() == PARENTHESES_EXPR ? parensBySemicolon(tree, patternValues) : List.of();

        final int valuesSize = values.size();
        final int variablesSize = variables.size();

        final List<IndexEntry> res = new ArrayList<>(variablesSize);
        for (int i = 0; i < variablesSize; i++) {
            final LighterASTNode typedVariable = variables.get(i);
            if (typedVariable == null) {
                continue;
            }

            final LighterASTNode varDeclaration = LightTreeUtil.firstChildOfType(tree, typedVariable, VAR_DECLARATION);
            if (varDeclaration != null) {
                final LighterASTNode expression = i < valuesSize ? values.get(i) : null;
                res.add(createEntry(tree, varDeclaration, expression, text));
            }
        }
        return res;
    }

    /**
     * <pre>
     * QAssignmentExprImpl(ASSIGNMENT_EXPR)
     *      QVarIndexingImpl(VAR_INDEXING)
     *          QVarReferenceImpl(VAR_REFERENCE) - namespace
     *          QArgumentsImpl(ARGUMENTS)
     *              QLiteralExprImpl(LITERAL_EXPR)
     *                  QSymbolImpl(SYMBOL) - variable name
     * </pre>
     */
    private IndexEntry processIndexingAssignment(LighterAST tree, LighterASTNode var, CharSequence text) {
        final List<LighterASTNode> children = tree.getChildren(var);
        if (children.size() != 2) {
            return null;
        }

        final LighterASTNode namespaceNode = children.get(0);
        if (isNotOfType(namespaceNode, VAR_REFERENCE)) {
            return null;
        }

        final LighterASTNode argumentsNode = children.get(1);
        if (isNotOfType(argumentsNode, ARGUMENTS)) {
            return null;
        }

        final List<LighterASTNode> argNodes = tree.getChildren(argumentsNode);
        if (argNodes.size() != 3) {
            return null;
        }

        final LighterASTNode literalNode = argNodes.get(1);
        if (isNotOfType(literalNode, LITERAL_EXPR)) {
            return null;
        }

        final LighterASTNode symbol = LightTreeUtil.firstChildOfType(tree, literalNode, SYMBOL);
        if (symbol == null) {
            return null;
        }

        final TextRange range = new TextRange(symbol.getStartOffset() + 1, symbol.getEndOffset());
        CharSequence namespace = getQualifiedName(tree, namespaceNode, text);
        CharSequence varName = text.subSequence(range.getStartOffset(), range.getEndOffset());
        return new IndexEntry(namespace + "." + varName, IdentifierType.VARIABLE, range);
    }

    private @NotNull IndexEntry createEntry(@NotNull LighterAST tree, @NotNull LighterASTNode varDeclaration, @Nullable LighterASTNode expression, CharSequence text) {
        final String qualifiedName = getQualifiedName(tree, varDeclaration, text);
        final TextRange range = new TextRange(varDeclaration.getStartOffset(), varDeclaration.getEndOffset());
        return new IndexEntry(qualifiedName, IdentifierType.getType(expression), range);
    }

    private String getQualifiedName(LighterAST tree, LighterASTNode varDeclaration, CharSequence text) {
        final String name = getVariableName(text, varDeclaration);
        if (name.charAt(0) == '.') {
            return name;
        }

        final LighterASTNode context = findParent(tree, varDeclaration, CONTEXT_SCOPE);
        if (context == null) {
            return name;
        }

        final LighterASTNode ctxNameVar = LightTreeUtil.firstChildOfType(tree, context, VAR_DECLARATION);
        if (ctxNameVar == null) {
            return name;
        }

        final String ctxName = getVariableName(text, ctxNameVar);
        if (ctxName.trim().equals(".")) {
            return name;
        }
        return ctxName + '.' + name;
    }

    private String getVariableName(CharSequence text, LighterASTNode var) {
        return String.valueOf(text.subSequence(var.getStartOffset(), var.getEndOffset()));
    }

    private boolean isLocal(LighterAST tree, LighterASTNode var, int offset, CharSequence text) {
        if (text.charAt(var.getStartOffset()) == '.') {
            return false;
        }
        if (offset < text.length() - 1 && text.charAt(offset) == ':' && text.charAt(offset + 1) == ':') {
            return false;
        }
        return findParent(tree, var, LOCAL_VARIABLE_SCOPE) != null;
    }

    private LighterASTNode findParent(LighterAST tree, LighterASTNode item, TokenSet types) {
        LighterASTNode node = item;
        while (node != null) {
            if (types.contains(node.getTokenType())) {
                return node;
            }
            node = tree.getParent(node);
        }
        return null;
    }

    private boolean isNotOfType(LighterASTNode node, IElementType type) {
        return node == null || node.getTokenType() != type;
    }

    static List<IndexEntry> wrap(IndexEntry entry) {
        return entry == null ? null : List.of(entry);
    }

    static int[] findAllOffsets(CharSequence text) {
        // we have only chars - no reason for complex logic, just iterating all chars
        final int count = text.length();
        final IntList indexes = new IntArrayList();
        for (int i = 0; i < count; i++) {
            final char c = text.charAt(i);
            if (c == '`') { // symbols
                indexes.add(i);
            } else if (c == ':' && (i > 0 && text.charAt(i - 1) != ':')) { // assignments
                // Ignore double colons - it's the same type, by the fact
                indexes.add(i);
            }
        }
        return indexes.toIntArray();
    }

    static boolean containsSetBackward(CharSequence text, int startPosition) {
        int i = startPosition - 1; // ignore symbol char `

        // find bracket ignoring spaces
        for (; i >= 0; i--) {
            final char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (ch == '[') {
                i--;
                break;
            } else {
                // no bracket found
                return false;
            }
        }

        // ignore spaces
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
            i--;
        }
        // inverted 'set' and one space before or beginning of the text
        return i >= 2 && text.charAt(i) == 't' && text.charAt(i - 1) == 'e' && text.charAt(i - 2) == 's' && (i == 2 || Character.isWhitespace(text.charAt(i - 3)));
    }

    static boolean containsSetForward(CharSequence text, int startPosition) {
        int i = startPosition;
        final int length = text.length();
        while (i < length && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i + 3 <= length && text.charAt(i) == 's' && text.charAt(i + 1) == 'e' && text.charAt(i + 2) == 't' && (i + 3 == length || Character.isWhitespace(text.charAt(i + 3)));
    }

    static List<LighterASTNode> parensBySemicolon(LighterAST tree, LighterASTNode node) {
        final List<LighterASTNode> children = tree.getChildren(node);

        LighterASTNode currentNode = null;
        List<LighterASTNode> res = new ArrayList<>(children.size());
        for (LighterASTNode re : children) {
            final IElementType tokenType = re.getTokenType();
            if (tokenType == PAREN_CLOSE || tokenType == SEMICOLON) {
                res.add(currentNode);
                currentNode = null;
            } else if (tokenType == DIRECT_TYPED_VARIABLE || tokenType == INVERTED_TYPED_VARIABLE) {
                currentNode = re;
            }
        }
        return res;
    }

    private record IndexEntry(String name, IdentifierType type, TextRange range) {
    }
}
