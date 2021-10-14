package org.kdb.inside.brains.psi.index;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.lexer.FlexAdapter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.QLexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QDataIndexer2 implements DataIndexer<String, List<IdentifierDescriptor>, FileContent> {
    protected static final int VERSION = 14; // was 9

    public static final @NotNull TokenSet COLUMNS_TOKEN = TokenSet.create(QUERY_COLUMN);

    private static final Logger log = Logger.getInstance(QDataIndexer2.class);

    @Override
    public @NotNull Map<String, List<IdentifierDescriptor>> map(@NotNull FileContent content) {
        final FileType fileType = content.getFileType();
        if (!(fileType instanceof QFileType)) {
            return new HashMap<>();
        }
        log.info("Start indexing " + content.getFile());

        final CharSequence text = content.getContentAsText();
        final Map<String, List<IdentifierDescriptor>> res = new HashMap<>();

        FlexAdapter lexer = new FlexAdapter(new QLexer());
        lexer.start(text);
        while (lexer.getTokenStart() < lexer.getBufferEnd()) {
            IElementType token = lexer.getTokenType();
            if (token == VARIABLE_PATTERN) {
                log.info("  var dec: " + lexer.getTokenText());
            } else if (token == COLON) {
                log.info("  assignment found");
            }
            lexer.advance();
        }

/*

        final LighterAST tree = ((PsiDependentFileContent) content).getLighterAST();
        log.info("  light tree has been created");

        final List<LighterASTNode> children1 = LightTreeUtil.getChildrenOfType(tree, tree.getRoot(), ASSIGNMENT_EXPR);
        log.info("  found children: " + children1.size());
*/
/*

        final CharSequence text = content.getContentAsText();
        log.info("  text length: " + text.length());
        final int[] offsets = new StringSearcher(":", true, true).findAllOccurrences(text);
        log.info("  offsets count: " + offsets.length);
        if (offsets.length == 0) {
            return Collections.emptyMap();
        }


        AtomicInteger i = new AtomicInteger();

        LightTreeUtil.processLeavesAtOffsets(offsets, tree, (node, offset) -> {
            log.info("  processing offset " + i.incrementAndGet() + " of " + offsets.length + ": " + offset);
            final LighterASTNode assign = tree.getParent(node);
            log.info("      parent taken");
            if (assign == null) {
                return;
            }

            final IElementType tokenType = assign.getTokenType();
            log.info("      token type: " + tokenType);
            if (tokenType != ASSIGNMENT_EXPR) {
                return;
            }

            final List<LighterASTNode> children = tree.getChildren(assign);
            log.info("      children count: " + children.size());
            if (children.size() < 3) {
                return;
            }

            final LighterASTNode var = children.get(0);
            final IElementType varType = var.getTokenType();
            log.info("      var type: " + varType);
            if (varType != VAR_DECLARATION) {
                return;
            }

            // Ignore not global
            if (isLocal(tree, var, offset, text)) {
                log.info("      variable is local");
                return;
            }

            log.info("      variable is global. Extracting token...");
            final Token token = extractToken(tree, children);

            log.info("      variable token: " + token);
            final List<String> params = token.parameters.stream().map(n -> getVariableName(text, n)).collect(Collectors.toList());

            final TextRange r = new TextRange(var.getStartOffset(), var.getEndOffset());
            log.info("      text range: " + r);

            final String qualifiedName = getQualifiedName(tree, var, text);

            log.info("      qualified name: " + qualifiedName);
            res.computeIfAbsent(qualifiedName, n -> new ArrayList<>()).add(new IdentifierDescriptor(token.type, params, r));

            log.info("  offset done");
        });*/
        log.info("Indexing finished with " + res.size() + " keywords");
        return res;
    }

    @NotNull
    private QDataIndexer2.Token extractToken(LighterAST tree, List<LighterASTNode> children) {
        final LighterASTNode expression = children.get(children.size() - 1);
        final List<LighterASTNode> statements = tree.getChildren(expression);
        if (!statements.isEmpty()) {
            final LighterASTNode statement = statements.get(0);
            final IElementType tt = statement.getTokenType();
            if (tt == LAMBDA_EXPR) {
                List<LighterASTNode> params = LightTreeUtil.getChildrenOfType(tree, statement, PARAMETERS);
                if (params.size() == 1) {
                    params = LightTreeUtil.getChildrenOfType(tree, params.get(0), VAR_DECLARATION);
                }
                return new Token(IdentifierType.LAMBDA, params);
            } else if (tt == TABLE_EXPR) {
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
        return findParent(tree, var, LAMBDA_EXPR) != null || findParent(tree, var, TABLE_EXPR) != null;
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

        @Override
        public String toString() {
            return "Token{" +
                    "type=" + type +
                    ", parameters=" + parameters.size() +
                    '}';
        }
    }
}
