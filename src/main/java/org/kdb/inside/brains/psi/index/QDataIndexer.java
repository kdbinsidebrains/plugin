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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QDataIndexer implements DataIndexer<String, List<IdentifierDescriptor>, FileContent> {
    protected static final int VERSION = 16;

    private static final Logger log = Logger.getInstance(QDataIndexer.class);

    private static final TokenSet CONTEXT_SCOPE = TokenSet.create(CONTEXT);
    private static final TokenSet COLUMNS_TOKEN = TokenSet.create(TABLE_KEYS, TABLE_VALUES);
    private static final TokenSet LOCAL_VARIABLE_SCOPE = TokenSet.create(LAMBDA_EXPR, TABLE_EXPR);

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

            IndexEntry item = null;
            final IElementType tokenType = node.getTokenType();
            if (tokenType == SYMBOL) {
                item = processSymbol(tree, node, text);
            } else if (tokenType == COLUMN_ASSIGNMENT_TYPE || tokenType == VAR_ASSIGNMENT_TYPE || tokenType == VAR_ACCUMULATOR_TYPE) {
                item = processAssignment(tree, node, text, offset);
            }

            if (item != null) {
                log.info(fileName + ": index item generated - " + item);
                res.computeIfAbsent(item.name, i -> new ArrayList<>()).add(item.descriptor);
            }
        });
        final long finishedNanos = System.nanoTime();
        log.info("Indexing finished with " + res.size() + " keywords at " + (finishedNanos - startedNanos) + "ns");
        return res;
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

    private IndexEntry processSymbol(LighterAST tree, LighterASTNode node, CharSequence text) {
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
            return new IndexEntry(symbolValue, new IdentifierDescriptor(IdentifierType.VARIABLE, range));
        }
        return new IndexEntry(symbolValue, new IdentifierDescriptor(IdentifierType.SYMBOL, range));
    }

    private IndexEntry processAssignment(LighterAST tree, LighterASTNode node, CharSequence text, Integer offset) {
        final LighterASTNode parent = tree.getParent(node);
        if (parent == null) {
            return null;
        }

        final List<LighterASTNode> children = tree.getChildren(parent);
        if (children.size() < 3) {
            return null;
        }

        final LighterASTNode var = children.get(0);
        final IElementType varType = var.getTokenType();
        if (varType != VAR_DECLARATION && varType != VAR_INDEXING) {
            return null;
        }

        // Ignore not global
        if (isLocal(tree, var, offset, text)) {
            return null;
        }

        if (varType == VAR_INDEXING) {
            return processIndexingAssignment(tree, var, text);
        }
        return processDeclarationAssignment(tree, var, children, text);
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
        return new IndexEntry(namespace + "." + varName, new IdentifierDescriptor(IdentifierType.VARIABLE, range));
    }

    private @NotNull IndexEntry processDeclarationAssignment(LighterAST tree, LighterASTNode var, List<LighterASTNode> children, CharSequence text) {
        final Token token = extractToken(tree, children);
        final List<String> params = token.parameters.stream().map(n -> getVariableName(text, n)).collect(Collectors.toList());

        final String qualifiedName = getQualifiedName(tree, var, text);
        final TextRange range = new TextRange(var.getStartOffset(), var.getEndOffset());

        return new IndexEntry(qualifiedName, new IdentifierDescriptor(token.type, range, params));
    }

    @NotNull
    private QDataIndexer.Token extractToken(LighterAST tree, List<LighterASTNode> children) {
        final LighterASTNode expression = children.get(children.size() - 1);
        final IElementType tt = expression.getTokenType();
        if (tt == LAMBDA_EXPR) {
            List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, expression, PARAMETERS);
            if (params.size() == 1) {
                params = LightTreeUtil.getChildrenOfType(tree, params.get(0), VAR_DECLARATION);
            }
            return new Token(IdentifierType.LAMBDA, params);
        }
        if (tt == TABLE_EXPR) {
            final List<LighterASTNode> allColumns = LightTreeUtil.getChildrenOfType(tree, expression, COLUMNS_TOKEN);
            final List<LighterASTNode> columns = allColumns.stream()
                    .flatMap(c -> LightTreeUtil.getChildrenOfType(tree, c, TABLE_COLUMN).stream())
                    .flatMap(a -> LightTreeUtil.getChildrenOfType(tree, a, VAR_DECLARATION).stream())
                    .collect(Collectors.toList());
            return new Token(IdentifierType.TABLE, columns);
        }
        return new Token(IdentifierType.VARIABLE);
    }

    private String getQualifiedName(LighterAST tree, LighterASTNode var, CharSequence text) {
        final String name = getVariableName(text, var);
        if (name.charAt(0) == '.') {
            return name;
        }

        final LighterASTNode context = findParent(tree, var, CONTEXT_SCOPE);
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

    private record IndexEntry(String name, IdentifierDescriptor descriptor) {
    }

    static class Token {
        private final IdentifierType type;
        private final List<LighterASTNode> parameters;

        public Token(IdentifierType type) {
            this(type, List.of());
        }

        public Token(IdentifierType type, List<LighterASTNode> parameters) {
            this.type = type;
            this.parameters = parameters;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type=" + type +
                    ", parameters=" + parameters.size() +
                    '}';
        }
    }
}
