package org.kdb.inside.brains.lang.template;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

public class QTemplateContextType extends TemplateContextType {
    protected QTemplateContextType() {
        super("KDB+ Q");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        return QFileType.is(templateActionContext.getFile());
    }
}
