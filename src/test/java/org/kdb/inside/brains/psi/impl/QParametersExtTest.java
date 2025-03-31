package org.kdb.inside.brains.psi.impl;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.psi.QParameter;
import org.kdb.inside.brains.psi.QParameters;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class QParametersExtTest {
    @Test
    void parameterInfo() {
        final QParameters parameters = mock(QParameters.class, InvocationOnMock::callRealMethod);

        doReturn(List.of()).when(parameters).getParameters();
        assertEquals("[]", parameters.getParametersInfo());

        doReturn(List.of(createParameter("v1"), createParameter("v2"))).when(parameters).getParameters();
        assertEquals("[v1;v2]", parameters.getParametersInfo());
    }

    public static QParameter createParameter(String info) {
        final QParameter v = mock(QParameter.class);
        when(v.getParameterInfo()).thenReturn(info);
        return v;
    }
}