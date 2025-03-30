package org.kdb.inside.brains.psi.index;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.kdb.inside.brains.psi.QTypes.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class QDataIndexerTest {
    @Test
    void containsSetBackward() {
        checkBackward("set[`sdfdfasdf;10]");
        checkBackward(" set[`sdfdfasdf;10]");
        checkBackward(" set [`sdfdfasdf;10]");
        checkBackward(" set [ `sdfdfasdf;10]");
        checkBackward("  set  [  `sdfdfasdf;10]");
        checkBackward("\n  set\n  [\n  `sdfdfasdf;10]");
    }

    @Test
    void containsSetForward() {
        checkSetForward("`sdfdfasdf set");
        checkSetForward(" `sdfdfasdf   set");
        checkSetForward("\n `sdfdfasdf\n   set");
    }

    @Test
    void findIndexes() {
        assertArrayEquals(new int[]{4}, QDataIndexer.findAllOffsets("asd : 10;"));
        assertArrayEquals(new int[]{0}, QDataIndexer.findAllOffsets("`asd set 10;"));
        assertArrayEquals(new int[]{10, 14}, QDataIndexer.findAllOffsets(".asd.qwe[ `x ]: 10;"));
        assertArrayEquals(new int[]{10, 16, 20}, QDataIndexer.findAllOffsets(".asd.qwe[ `x ][ `y ]: 10;"));
    }

    @Test
    void parensBySemicolon() {
        final LighterASTNode symbol = node(SYMBOL);
        final LighterASTNode lambda = node(LAMBDA_EXPR);

        final LighterAST tree = mock(LighterAST.class);
        final LighterASTNode node = mock(LighterASTNode.class);
        final List<LighterASTNode> children = List.of(
                node(PAREN_OPEN),
                node(SEMICOLON),
                symbol,
                node(SEMICOLON),
                node(SEMICOLON),
                lambda,
                node(PAREN_CLOSE)
        );
        when(tree.getChildren(node)).thenReturn(children);

        final List<LighterASTNode> nodes = QDataIndexer.parensBySemicolon(tree, node);
        assertIterableEquals(Arrays.asList(null, symbol, null, lambda), nodes);
    }

    void checkBackward(String s) {
        assertTrue(QDataIndexer.containsSetBackward(s, s.indexOf('`')));
    }

    void checkSetForward(String s) {
        final int startPosition = s.indexOf('`');
        assertTrue(QDataIndexer.containsSetForward(s, s.indexOf(' ', startPosition + 1)));
    }

    LighterASTNode node(IElementType type) {
        final LighterASTNode node = mock(LighterASTNode.class);
        when(node.getTokenType()).thenReturn(type);
        return node;
    }
}