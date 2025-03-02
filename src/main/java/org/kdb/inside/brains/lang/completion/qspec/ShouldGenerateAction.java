package org.kdb.inside.brains.lang.completion.qspec;

import org.kdb.inside.brains.lang.qspec.TestDescriptor;

public class ShouldGenerateAction extends BaseQSpecGenerateAction {
    public ShouldGenerateAction() {
        super(TestDescriptor.SHOULD, "[\"\"]");
    }
}
