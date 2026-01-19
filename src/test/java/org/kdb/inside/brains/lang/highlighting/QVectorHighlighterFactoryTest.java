package org.kdb.inside.brains.lang.highlighting;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.EnforceWriteAction;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.QLanguageTestCase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QVectorHighlighterFactoryTest extends QLanguageTestCase {
    @Test
    @EnforceWriteAction
    void incorrect() {
        // https://code.kx.com/q/ref/enkey/
        assertUndefined(fixture("1!`c`d"), 0);

        // https://code.kx.com/q/ref/enkey/#unkey
        assertUndefined(fixture("0!`c`d"), 0);

        // https://code.kx.com/q/ref/enumeration/
        assertUndefined(fixture("x!`c`d"), 0);

        // https://code.kx.com/q/ref/flip-splayed/
        assertUndefined(fixture("enlist[`a]!`t"), 0);

        // https://code.kx.com/q/ref/display/
        assertUndefined(fixture("0N!x"), 0);

        // https://code.kx.com/q/basics/internal/
        assertUndefined(fixture("-2!x"), 0);

        // https://code.kx.com/q/basics/funsql/
        assertUndefined(fixture("![t;c;b;a]"), 0);
    }

    @Test
    @EnforceWriteAction
    void symbols() {
        final CodeInsightTestFixture fixture = fixture("`a`b!`c`d");

        assertPosition(fixture, 1, new TextRange(0, 2), new TextRange(5, 7));
        assertPosition(fixture, 3, new TextRange(2, 4), new TextRange(7, 9));
        assertPosition(fixture, 5, new TextRange(5, 7), new TextRange(0, 2));
        assertPosition(fixture, 7, new TextRange(7, 9), new TextRange(2, 4));
    }

    @Test
    @EnforceWriteAction
    void incompleteSymbols() {
        final CodeInsightTestFixture fixture = fixture("`a`b`d!`c`d");

        assertPosition(fixture, 1, new TextRange(0, 2), new TextRange(7, 9));
        assertPosition(fixture, 3, new TextRange(2, 4), new TextRange(9, 11));
        assertPosition(fixture, 5);
        assertPosition(fixture, 7, new TextRange(7, 9), new TextRange(0, 2));
        assertPosition(fixture, 9, new TextRange(9, 11), new TextRange(2, 4));
    }

    @Test
    @EnforceWriteAction
    void parentheses() {
        final CodeInsightTestFixture fixture = fixture("(`a;10)!(20;`b)");

        assertPosition(fixture, 1, new TextRange(1, 3), new TextRange(9, 11));
        assertPosition(fixture, 9, new TextRange(9, 11), new TextRange(1, 3));
        assertPosition(fixture, 4, new TextRange(4, 6), new TextRange(12, 14));
        assertPosition(fixture, 13, new TextRange(12, 14), new TextRange(4, 6));
    }

    @Test
    @EnforceWriteAction
    void vector() {
        final CodeInsightTestFixture fixture = fixture("`a`b`c!10 30 40");

//        0x123456!10 20 30

        assertPosition(fixture, 0, new TextRange(0, 2), new TextRange(7, 9));
        assertPosition(fixture, 1, new TextRange(0, 2), new TextRange(7, 9));
        assertPosition(fixture, 2, new TextRange(2, 4), new TextRange(10, 12));
        assertPosition(fixture, 3, new TextRange(2, 4), new TextRange(10, 12));
        assertPosition(fixture, 4, new TextRange(4, 6), new TextRange(13, 15));
        assertPosition(fixture, 5, new TextRange(4, 6), new TextRange(13, 15));
    }

    @Test
    @EnforceWriteAction
    void vectorHex() {
        final CodeInsightTestFixture f1 = fixture("`a`b`c!0x123456");
        assertPosition(f1, 0, new TextRange(0, 2), new TextRange(9, 11));
        assertPosition(f1, 2, new TextRange(2, 4), new TextRange(11, 13));
        assertPosition(f1, 4, new TextRange(4, 6), new TextRange(13, 15));

        final CodeInsightTestFixture f2 = fixture("`a`b`c!0x23456");
        assertPosition(f2, 0, new TextRange(0, 2), new TextRange(9, 10));
        assertPosition(f2, 2, new TextRange(2, 4), new TextRange(10, 12));
        assertPosition(f2, 4, new TextRange(4, 6), new TextRange(12, 14));
    }

    @Test
    @EnforceWriteAction
    void vectorBoolean() {
        final CodeInsightTestFixture fixture = fixture("`a`b`c!101b");
        assertPosition(fixture, 0, new TextRange(0, 2), new TextRange(7, 8));
        assertPosition(fixture, 2, new TextRange(2, 4), new TextRange(8, 9));
        assertPosition(fixture, 4, new TextRange(4, 6), new TextRange(9, 10));
    }

    @Test
    @EnforceWriteAction
    void vectorWithEnding() {
        final CodeInsightTestFixture fixture = fixture("`a`b`c!1 2 3f");
        assertPosition(fixture, 0, new TextRange(0, 2), new TextRange(7, 8));
        assertPosition(fixture, 2, new TextRange(2, 4), new TextRange(9, 10));
        assertPosition(fixture, 4, new TextRange(4, 6), new TextRange(11, 12));
    }

    private void assertUndefined(CodeInsightTestFixture fixture, int position) {
        assertNull(createHandler(fixture, position));
    }

    private void assertPosition(CodeInsightTestFixture fixture, int pos, TextRange... ranges) {
        final List<@NotNull TextRange> u1 = highlight(fixture, pos);
        assertEquals(ranges.length, u1.size());
        for (int i = 0; i < ranges.length; i++) {
            assertEquals(ranges[i], u1.get(i), "Position " + i + " does not match expected range " + ranges[i]);
        }
    }

    private CodeInsightTestFixture fixture(String code) {
        final CodeInsightTestFixture fixture = getFixture();
        fixture.configureByText(QFileType.INSTANCE, code);
        return fixture;
    }

    private @NotNull List<TextRange> highlight(CodeInsightTestFixture fixture, int position) {
        final HighlightUsagesHandlerBase<PsiElement> h1 = createHandler(fixture, position);
        assertNotNull(h1);

        h1.computeUsages(List.of());
        return h1.getReadUsages();
    }

    private static @Nullable HighlightUsagesHandlerBase<PsiElement> createHandler(CodeInsightTestFixture fixture, int position) {
        fixture.getEditor().getCaretModel().moveToOffset(position, true);
        final PsiElement el = fixture.getFile().findElementAt(position);

        final QVectorHighlighterFactory factory = new QVectorHighlighterFactory();
        return factory.createHighlightUsagesHandler(fixture.getEditor(), fixture.getFile(), el);
    }
}