package org.kdb.inside.brains.view.struct;

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QPsiElement;

import javax.swing.*;
import java.util.function.Function;

public class QNavBarExtension extends StructureAwareNavBarModelExtension {
    @NotNull
    @Override
    protected Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @Nullable Icon getIcon(Object object) {
        return extract(object, QStructureViewElement::getBaseIcon);
    }

    @Override
    public @Nullable String getPresentableText(Object object) {
        return extract(object, QStructureViewElement::getPresentableText);
    }

    private <T> T extract(Object object, Function<QStructureViewElement, T> function) {
        if (object instanceof QPsiElement) {
            final QStructureViewElement viewElement = QStructureViewElement.createViewElement((PsiElement) object);
            return viewElement != null ? function.apply(viewElement) : null;
        }
        return null;
    }
}
