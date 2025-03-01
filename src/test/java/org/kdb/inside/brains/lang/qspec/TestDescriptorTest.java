package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QInvokeFunction;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDescriptorTest {
    private static @NotNull TestItem item(String name) {
        final QFile f = mock(QFile.class);
        final VirtualFile vf = mock(VirtualFile.class);
        final Path path = Path.of("c:\\mock\\file\\path");
        when(vf.toNioPath()).thenReturn(path);
        when(f.getVirtualFile()).thenReturn(vf);

        QInvokeFunction invoke = mock(QInvokeFunction.class);
        when(invoke.getContainingFile()).thenReturn(f);

        TestItem mock = mock(TestItem.class);
        when(mock.getCaption()).thenReturn(name);
        when(mock.getInvoke()).thenReturn(invoke);
        return mock;
    }

    @Test
    public void createUrl() {
//        assertEquals("qspec:script://c:/mock/file/path", new TestDescriptor(f, null, null, false).createUrl());
//        assertEquals("qspec:test://c:/mock/file/path?[]/[my testCase]", new TestDescriptor(f, null, item("my testCase"), false).createUrl());
        assertEquals("qspec:suite://c:/mock/file/path?[my test]", new TestDescriptor(item("my test"), null, false).createUrl());
        assertEquals("qspec:test://c:/mock/file/path?[my test]/[my testCase]", new TestDescriptor(item("my test"), item("my testCase"), false).createUrl());
    }
}