package org.kdb.inside.brains.view.struct;

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QPsiElement;

import javax.swing.*;

public class QNavBarExtension extends StructureAwareNavBarModelExtension {
    @NotNull
    @Override
    protected Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @Nullable Icon getIcon(Object object) {
        if (object instanceof QPsiElement) {
            return ((QPsiElement) object).getIcon(0);
        }
        return null;
    }

    @Override
    public @Nullable String getPresentableText(Object object) {
        if (object instanceof QPsiElement) {
            final QStructureViewElement element = QStructureViewElement.createViewElement((PsiElement) object);
            return element != null ? element.getPresentableText() : null;
        }
        return null;
    }
}
