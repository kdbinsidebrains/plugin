package org.kdb.inside.brains.lang.completion.qspec;

import org.kdb.inside.brains.lang.qspec.TestDescriptor;

public class HoldsGenerateAction extends BaseQSpecGenerateAction {
    public HoldsGenerateAction() {
        super(TestDescriptor.HOLDS, "[\"\";()!()]");
    }
}
