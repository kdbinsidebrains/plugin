package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QInvokeFunction;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDescriptorTest {
    static final Path path = Path.of("test_path");

    private static @NotNull TestItem item(String name) {
        final QFile f = mock(QFile.class);
        final VirtualFile vf = mock(VirtualFile.class);
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
        final String script = FilenameUtils.normalize(path.toAbsolutePath().toString(), true);

        assertEquals("qspec:suite://" + script + "?[my test]", new TestDescriptor(item("my test"), null, null).createUrl());
        assertEquals("qspec:test://" + script + "?[my test]/[my testCase]", new TestDescriptor(item("my test"), item("my testCase"), null).createUrl());
    }
}