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
    protected static final int VERSION = 1;

    public static final @NotNull TokenSet COLUMNS_TOKEN = TokenSet.create(KEY_COLUMNS, VALUE_COLUMNS);

    @Override
    public @NotNull Map<String, List<IdentifierDescriptor>> map(@NotNull FileContent content) {
        final Map<String, List<IdentifierDescriptor>> res = new HashMap<>();

        final CharSequence text = content.getContentAsText();
        final int[] offsets = new StringSearcher(":", true, true).findAllOccurrences(text);
/*
        int[] offsets = ArrayUtil.mergeArrays(
                new StringSearcher(":", true, true).findAllOccurrences(text),
                new StringSearcher("::", true, true).findAllOccurrences(text));
*/
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
            if (var.getTokenType() != VARIABLE) {
                return;
            }

            // Ignore not global
            final boolean global = isGlobal(tree, assign, text, offset);
            if (!global) {
                return;
            }

            final Variable type = getType(tree, children);
            final String qualifiedName = getQualifiedName(tree, var, text);
            final List<String> params = type.parameters.stream().map(n -> getVariableName(text, n)).collect(Collectors.toList());

            final TextRange r = new TextRange(var.getStartOffset(), var.getEndOffset());
            res.computeIfAbsent(qualifiedName, n -> new ArrayList<>()).add(new IdentifierDescriptor(type.type, params, r));
        });
        return res;
    }

    @NotNull
    private Variable getType(LighterAST tree, List<LighterASTNode> children) {
        final LighterASTNode exp = children.get(children.size() - 1);
        final List<LighterASTNode> vals = tree.getChildren(exp);
        if (!vals.isEmpty()) {
            final LighterASTNode node = vals.get(0);
            final IElementType tt = node.getTokenType();
            if (tt == PRIMITIVE) {
                final LighterASTNode table = tree.getChildren(node).get(0);
                if (table.getTokenType() == TABLE) {
                    final List<LighterASTNode> columns = LightTreeUtil.getChildrenOfType(tree, table, COLUMNS_TOKEN).stream()
                            .flatMap(c -> LightTreeUtil.getChildrenOfType(tree, c, COLUMN_ASSIGNMENT).stream())
                            .flatMap(a -> LightTreeUtil.getChildrenOfType(tree, a, VARIABLE).stream())
                            .collect(Collectors.toList());
                    return new Variable(IdentifierType.TABLE, columns);
                }
            } else if (tt == LAMBDA) {
                List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, node, PARAMETERS);
                if (params.size() == 1) {
                    params = LightTreeUtil.getChildrenOfType(tree, params.get(0), VARIABLE);
                }
                return new Variable(IdentifierType.LAMBDA, params);
            } else {
                return new Variable(IdentifierType.VARIABLE);
            }
        }
        return new Variable(IdentifierType.VARIABLE);
    }

    private String getQualifiedName(LighterAST tree, LighterASTNode var, CharSequence text) {
        final String name = getVariableName(text, var);

        final LighterASTNode context = findParent(tree, var, CONTEXT);
        if (context == null) {
            return name;
        }

        final LighterASTNode ctxNameVar = LightTreeUtil.firstChildOfType(tree, context, VARIABLE);
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

    private boolean isGlobal(LighterAST tree, LighterASTNode item, CharSequence text, int offset) {
        if (offset < text.length() - 1 && text.charAt(offset) == ':' && text.charAt(offset + 1) == ':') {
            return true;
        }
        return findParent(tree, item, LAMBDA) == null;
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

    static class Variable {
        private final IdentifierType type;
        private final List<LighterASTNode> parameters;

        public Variable(IdentifierType type) {
            this(type, List.of());
        }

        public Variable(IdentifierType type, List<LighterASTNode> parameters) {
            this.type = type;
            this.parameters = parameters;
        }
    }
}
