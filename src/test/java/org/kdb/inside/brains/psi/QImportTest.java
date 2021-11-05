package org.kdb.inside.brains.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QImportTest {
    @Test
    void empty() {
        assertImport("\\l", "", TextRange.EMPTY_RANGE);
        assertImport("\\l ", "", TextRange.EMPTY_RANGE);
        assertImport("\\l      ", "", TextRange.EMPTY_RANGE);
        assertImport("system \"l\"", "", TextRange.EMPTY_RANGE);
        assertImport("system \"l \"", "", TextRange.EMPTY_RANGE);
        assertImport("system \"l            \"", "", TextRange.EMPTY_RANGE);
    }

    @Test
    void command() {
        assertImport("\\l asd.q", "asd.q", new TextRange(3, 8));
        assertImport("   \\l     asd.q   ", "asd.q", new TextRange(10, 15));
    }

    @Test
    void function() {
        assertImport("system \"l asd.q\"", "asd.q", new TextRange(10, 15));
        assertImport("    system    \"   l    asd.q    \"    ", "asd.q", new TextRange(23, 28));
    }

    private void assertImport(String s, String text, TextRange range) {
        final MockImport mockImport = new MockImport(s);
        assertEquals(text, mockImport.getFilePath());
        assertEquals(text, mockImport.getFilePathRange().substring(mockImport.getText()));
        assertEquals(range, mockImport.getFilePathRange());
    }

    private static class MockImport extends FakePsiElement implements QImport {
        private final String text;

        private MockImport(String text) {
            this.text = text;
        }

        @Override
        public @NotNull String getText() {
            return text;
        }

        @Override
        public PsiElement getParent() {
            return null;
        }
    }
}