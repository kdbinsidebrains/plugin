package org.kdb.inside.brains.psi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QPsiUtilTest {
    @Test
    void getImportContent() {
        final QExpression expr = mock();
        final QImportFunction f = mock(QImportFunction.class);
        when(f.getExpression()).thenReturn(expr);

        when(expr.getText()).thenReturn("\"l mock.q\"");
        assertEquals("mock.q", QPsiUtil.getImportContent(f));

        when(expr.getText()).thenReturn("\"l \",var");
        assertEquals("var", QPsiUtil.getImportContent(f));

        when(expr.getText()).thenReturn("\"l asd/qwe/\",var");
        assertEquals("\"asd/qwe/\",var", QPsiUtil.getImportContent(f));

        final QImportCommand cmd = mock(QImportCommand.class);
        when(cmd.getText()).thenReturn("\\l asd/qwe.q");
        when(cmd.getFilePath()).thenCallRealMethod();
        when(cmd.getFilePathRange()).thenCallRealMethod();
        assertEquals("asd/qwe.q", QPsiUtil.getImportContent(cmd));
    }
}