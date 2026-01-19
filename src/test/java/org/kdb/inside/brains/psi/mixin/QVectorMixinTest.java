package org.kdb.inside.brains.psi.mixin;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.EnforceWriteAction;
import org.kdb.inside.brains.QLanguageTestCase;
import org.kdb.inside.brains.psi.QVector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QVectorMixinTest extends QLanguageTestCase {
    @Test
    @EnforceWriteAction
    public void testGetIndexForPosition_Hex() {
        // Even length: 0x112233
        QVector v1 = createVector("0x112233");
        assertEquals(0, v1.getIndexForPosition(2)); // '1'
        assertEquals(1, v1.getIndexForPosition(4)); // '2'
        assertEquals(2, v1.getIndexForPosition(6)); // '3'

        // Odd length: 0x12233 (first byte is '1')
        QVector v2 = createVector("0x12233");
        assertEquals(0, v2.getIndexForPosition(2)); // '1'
        assertEquals(1, v2.getIndexForPosition(3)); // '2'
    }

    @Test
    @EnforceWriteAction
    public void testGetIndexForPosition_Boolean() {
        QVector v = createVector("1011b");
        assertEquals(0, v.getIndexForPosition(0)); // '1'
        assertEquals(2, v.getIndexForPosition(2)); // '1'
        assertEquals(3, v.getIndexForPosition(3)); // '1'
    }

    @Test
    @EnforceWriteAction
    public void testGetIndexForPosition_Whitespace() {
        // Standard case: "1 2 3"
        QVector v1 = createVector("1 2 3");
        assertEquals(0, v1.getIndexForPosition(0)); // '1'
        assertEquals(0, v1.getIndexForPosition(1)); // ' '
        assertEquals(1, v1.getIndexForPosition(2)); // '2'
        assertEquals(1, v1.getIndexForPosition(3)); // ' '
        assertEquals(2, v1.getIndexForPosition(4)); // '3'

        // Trailing letter case: "10 20f"
        QVector v2 = createVector("10 20f");
        assertEquals(0, v2.getIndexForPosition(0)); // '1'
        assertEquals(1, v2.getIndexForPosition(3)); // '2'
        assertEquals(-1, v2.getIndexForPosition(5)); // 'f' (ignored)
    }

    @Test
    @EnforceWriteAction
    public void testGetRangeForIndex_Whitespace() {
        QVector v = createVector("10   20  ");

        // Index 0 should be "10"
        TextRange r0 = v.getRangeForIndex(0);
        assertEquals(new TextRange(0, 2), r0);

        // Index 1 should be "20"
        TextRange r1 = v.getRangeForIndex(1);
        assertEquals(new TextRange(5, 7), r1);
    }

    @Test
    @EnforceWriteAction
    public void testGetRangeForIndex_HexOdd() {
        QVector v = createVector("0x12233");

        assertEquals(new TextRange(2, 3), v.getRangeForIndex(0)); // "1"
        assertEquals(new TextRange(3, 5), v.getRangeForIndex(1)); // "22"
    }

    private QVector createVector(String text) {
        final JavaCodeInsightTestFixture fixture = getFixture();
        fixture.configureByText("test.q", text);
        PsiElement element = fixture.getFile().findElementAt(0);
        QVector vector = PsiTreeUtil.getParentOfType(element, QVector.class, false);
        assertNotNull(vector, "Could not find QVector in PSI for text: " + text);
        return vector;
    }
}