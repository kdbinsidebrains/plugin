package org.kdb.inside.brains.psi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class QPsiUtilTest {
    public static QVarDeclaration createVarDeclaration(String name) {
        final QVarDeclaration v = mock(QVarDeclaration.class);
        when(v.getText()).thenReturn(name);
        when(v.getName()).thenReturn(name);
        when(v.getQualifiedName()).thenReturn(name);
        doCallRealMethod().when(v).getSimpleName();
        return v;
    }

    @Test
    void getLambdaDescriptor() {
        final QLambdaExpr lambda = mock(QLambdaExpr.class);
        assertEquals("mock[]", QPsiUtil.getLambdaDescriptor("mock", lambda));

        final QParameters parameters = mock(QParameters.class);
        when(lambda.getParameters()).thenReturn(parameters);
        assertEquals("mock[]", QPsiUtil.getLambdaDescriptor("mock", lambda));

        doReturn(List.of(createVarDeclaration("v1"), createVarDeclaration("v2"))).when(parameters).getVariables();
        assertEquals("mock[v1;v2]", QPsiUtil.getLambdaDescriptor("mock", lambda));
    }

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