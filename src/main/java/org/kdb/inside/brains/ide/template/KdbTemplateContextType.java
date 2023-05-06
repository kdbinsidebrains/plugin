package org.kdb.inside.brains.ide.template;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

public class KdbTemplateContextType extends TemplateContextType {
    protected KdbTemplateContextType() {
        super("KDB+ Q");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        return QFileType.is(templateActionContext.getFile());
    }
}
