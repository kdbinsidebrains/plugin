package org.kdb.inside.brains.view.struct;

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.Optional;

/**
 * See JavaNavBarExtension as an example
 */
public class QNavBarExtension extends StructureAwareNavBarModelExtension {
    @NotNull
    @Override
    protected Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @Nullable Icon getIcon(Object object) {
        return getElement(object).map(e -> e.getIcon(false)).orElse(null);
    }

    @Override
    public @Nullable String getPresentableText(Object object) {
        return getElement(object).map(QStructureViewElement::getPresentableText).orElse(null);
    }

    @Override
    public @Nullable PsiElement getParent(@Nullable PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        }
        return ElementContext.of(psiElement).getElement();
    }

    private Optional<QStructureViewElement> getElement(Object object) {
        if (!(object instanceof QPsiElement psi)) {
            return Optional.empty();
        }
        final PsiElement target;
        if (psi instanceof QLambdaExpr || psi instanceof QTableExpr || psi instanceof QDictExpr) {
            final PsiElement parent = psi.getParent();
            target = parent instanceof QAssignmentExpr ? parent : psi;
        } else {
            target = psi;
        }
        return QStructureViewElement.createViewElement(target)
                .filter(e -> e instanceof QStructureViewElement)
                .map(e -> (QStructureViewElement) e)
                .findFirst();
    }
}