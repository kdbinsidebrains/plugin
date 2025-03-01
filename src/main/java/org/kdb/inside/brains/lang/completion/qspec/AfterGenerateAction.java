package org.kdb.inside.brains.lang.completion.qspec;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;

import java.util.List;

public class AfterGenerateAction extends BaseQSpecGenerateAction {
    public AfterGenerateAction() {
        super(TestDescriptor.AFTER);
    }

    @Override
    protected boolean isValidForTest(@NotNull Project project, @NotNull Editor editor, @NotNull TestDescriptor descriptor, @NotNull List<TestItem> items) {
        return items.stream().noneMatch(i -> i.is(TestDescriptor.AFTER));
    }
}
