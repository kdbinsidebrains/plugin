package org.kdb.inside.brains.lang.slicer;

import com.intellij.psi.PsiElement;
import com.intellij.slicer.SliceUsage;
import com.intellij.slicer.SliceUsageCellRendererBase;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.FontUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QVarDeclaration;

public class QSliceUsageCellRendererBase extends SliceUsageCellRendererBase {
    @Override
    public void customizeCellRendererFor(@NotNull SliceUsage sliceUsage) {
        final QSliceUsage usage = (QSliceUsage) sliceUsage;

        final PsiElement element = sliceUsage.getElement();
        if (!(element instanceof QVarDeclaration declaration)) {
            return;
        }

        setIcon(declaration.getIcon(0));
        append(String.valueOf(sliceUsage.getLine()), SimpleTextAttributes.GRAY_ATTRIBUTES);
        append(FontUtil.spaceAndThinSpace());
        append(declaration.getQualifiedName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        append(FontUtil.spaceAndThinSpace());
        append(usage.getFile().getName(), SimpleTextAttributes.GRAY_ATTRIBUTES);

        if (usage.isRecursion()) {
            append(FontUtil.spaceAndThinSpace());
            append("(recursion call)", SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
        }
    }
}