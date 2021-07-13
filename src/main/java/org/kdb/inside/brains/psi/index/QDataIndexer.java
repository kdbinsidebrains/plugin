package org.kdb.inside.brains.psi.index;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.PsiDependentFileContent;
import com.intellij.util.text.StringSearcher;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QDataIndexer implements DataIndexer<String, List<IdentifierDescriptor>, FileContent> {
    protected static final int VERSION = 9;

    public static final @NotNull TokenSet COLUMNS_TOKEN = TokenSet.create(KEY_COLUMNS, VALUE_COLUMNS);

    @Override
    public @NotNull Map<String, List<IdentifierDescriptor>> map(@NotNull FileContent content) {
        final Map<String, List<IdentifierDescriptor>> res = new HashMap<>();

        final CharSequence text = content.getContentAsText();
        final int[] offsets = new StringSearcher(":", true, true).findAllOccurrences(text);
        if (offsets.length == 0) {
            return Collections.emptyMap();
        }

        final LighterAST tree = ((PsiDependentFileContent) content).getLighterAST();

        LightTreeUtil.processLeavesAtOffsets(offsets, tree, (node, offset) -> {
            final LighterASTNode assign = tree.getParent(node);
            if (assign == null) {
                return;
            }

            final IElementType tokenType = assign.getTokenType();
            if (tokenType != VARIABLE_ASSIGNMENT) {
                return;
            }

            final List<LighterASTNode> children = tree.getChildren(assign);
            if (children.size() < 3) {
                return;
            }

            final LighterASTNode var = children.get(0);
            if (var.getTokenType() != VAR_DECLARATION) {
                return;
            }

            // Ignore not global
            if (isLocal(tree, var, offset, text)) {
                return;
            }

            final Token token = extractToken(tree, children);
            final List<String> params = token.parameters.stream().map(n -> getVariableName(text, n)).collect(Collectors.toList());

            final TextRange r = new TextRange(var.getStartOffset(), var.getEndOffset());

            final String qualifiedName = getQualifiedName(tree, var, text);
            res.computeIfAbsent(qualifiedName, n -> new ArrayList<>()).add(new IdentifierDescriptor(token.type, params, r));
        });
        return res;
    }

    @NotNull
    private QDataIndexer.Token extractToken(LighterAST tree, List<LighterASTNode> children) {
        final LighterASTNode expression = children.get(children.size() - 1);
        final List<LighterASTNode> statements = tree.getChildren(expression);
        if (!statements.isEmpty()) {
            final LighterASTNode statement = statements.get(0);
            final IElementType tt = statement.getTokenType();
            if (tt == LAMBDA) {
                List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, statement, PARAMETERS);
                if (params.size() == 1) {
                    params = LightTreeUtil.getChildrenOfType(tree, params.get(0), VAR_DECLARATION);
                }
                return new Token(IdentifierType.LAMBDA, params);
            } else if (tt == TABLE) {
                final List<LighterASTNode> columns = LightTreeUtil.getChildrenOfType(tree, statement, COLUMNS_TOKEN).stream()
                        .flatMap(c -> LightTreeUtil.getChildrenOfType(tree, c, TABLE_COLUMN).stream())
                        .flatMap(a -> LightTreeUtil.getChildrenOfType(tree, a, VAR_DECLARATION).stream())
                        .collect(Collectors.toList());
                return new Token(IdentifierType.TABLE, columns);
            }
        }
        return new Token(IdentifierType.VARIABLE);
    }

    private String getQualifiedName(LighterAST tree, LighterASTNode var, CharSequence text) {
        final String name = getVariableName(text, var);
        if (name.charAt(0) == '.') {
            return name;
        }

        final LighterASTNode context = findParent(tree, var, CONTEXT);
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
        return findParent(tree, var, LAMBDA) != null || findParent(tree, var, TABLE) != null;
    }

    private LighterASTNode findParent(LighterAST tree, LighterASTNode item, IElementType type) {
        LighterASTNode node = item;
        while (node != null) {
            if (node.getTokenType() == type) {
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
    }
}
