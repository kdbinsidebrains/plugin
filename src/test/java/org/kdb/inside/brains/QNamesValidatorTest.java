package org.kdb.inside.brains;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.lang.refactoring.QNamesValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QNamesValidatorTest {
    @Test
    public void test() {
        final QNamesValidator v = new QNamesValidator();
        assertTrue(v.isKeyword("if", null));

        assertTrue(v.isIdentifier("test", null));
        assertTrue(v.isIdentifier(".asd.qwe", null));
        assertFalse(v.isIdentifier("32234", null));
    }
}