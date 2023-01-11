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
    protected static final int VERSION = 14;

    private static final Logger log = Logger.getInstance(QDataIndexer.class);

    private static final TokenSet CONTEXT_SCOPE = TokenSet.create(CONTEXT);
    private static final TokenSet COLUMNS_TOKEN = TokenSet.create(TABLE_KEYS, TABLE_VALUES);
    private static final TokenSet LOCAL_VARIABLE_SCOPE = TokenSet.create(LAMBDA_EXPR, TABLE_EXPR);

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

            Map.Entry<String, IdentifierDescriptor> item = null;
            final IElementType tokenType = node.getTokenType();
            if (tokenType == SYMBOL) {
                item = processSymbol(node, text);
            } else if (tokenType == COLUMN_ASSIGNMENT_TYPE || tokenType == VAR_ASSIGNMENT_TYPE || tokenType == VAR_ACCUMULATOR_TYPE) {
                item = processAssignment(tree, node, text, offset);
            }

            if (item != null) {
                log.info(fileName + ": index item generated - " + item);
                res.computeIfAbsent(item.getKey(), i -> new ArrayList<>()).add(item.getValue());
            }
        });
        final long finishedNanos = System.nanoTime();
        log.info("Indexing finished with " + res.size() + " keywords at " + (finishedNanos - startedNanos) + "ns");
        return res;
    }

    private int[] findAllOffsets(CharSequence text) {
        // we have only chars - no reason for complex logic, just iterating all chars
        final int count = text.length();
        final IntList indexes = new IntArrayList();
        for (int i = 0; i < count; i++) {
            final char c = text.charAt(i);
            if (c == '`') {
                indexes.add(i);
            }
            // Ignore double colons - it's the same type, by the fact
            if (c == ':' && (i > 0 && text.charAt(i - 1) != ':')) {
                indexes.add(i);
            }
        }
        return indexes.toIntArray();
    }

    private Map.Entry<String, IdentifierDescriptor> processSymbol(LighterASTNode node, CharSequence text) {
        final TextRange range = new TextRange(node.getStartOffset() + 1, node.getEndOffset());
        final String symbolValue = range.subSequence(text).toString();

        // We ignore root namespace
        if (symbolValue.isEmpty() || symbolValue.equals(".")) {
            return null;
        }

        // We ignore
        if (symbolValue.startsWith(":")) {
            return null;
        }

        return new AbstractMap.SimpleEntry<>(symbolValue, new IdentifierDescriptor(IdentifierType.SYMBOL, List.of(), range));
    }

    private Map.Entry<String, IdentifierDescriptor> processAssignment(LighterAST tree, LighterASTNode node, CharSequence text, Integer offset) {
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
        if (varType != VAR_DECLARATION) {
            return null;
        }

        // Ignore not global
        if (isLocal(tree, var, offset, text)) {
            return null;
        }

        final Token token = extractToken(tree, children);
        final List<String> params = token.parameters.stream().map(n -> getVariableName(text, n)).collect(Collectors.toList());

        final String qualifiedName = getQualifiedName(tree, var, text);
        final TextRange range = new TextRange(var.getStartOffset(), var.getEndOffset());

        return new AbstractMap.SimpleEntry<>(qualifiedName, new IdentifierDescriptor(token.type, params, range));
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
