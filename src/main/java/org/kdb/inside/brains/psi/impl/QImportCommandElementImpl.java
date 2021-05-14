package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.kdb.inside.brains.psi.QImportElement;
import org.kdb.inside.brains.psi.QImportFile;

public class QImportCommandElementImpl extends QPsiElementImpl implements QImportElement {
    public QImportCommandElementImpl(ASTNode node) {
        super(node);
    }
/*

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @NotNull
            @Override
            public String getPresentableText() {
                final String text = getText();
                return text.substring(text.indexOf(' ') + 1).trim();
            }

            @NotNull
            @Override
            public String getLocationString() {
                final PsiFile containingFile = getContainingFile();
                return containingFile == null ? "" : containingFile.getName();
            }

            @NotNull
            @Override
            public Icon getIcon(boolean unused) {
                return KdbIcons.Main.Import;
            }
        };
    }
*/

    @Override
    public TextRange getFilepathRange() {
        final QImportFile childByClass = findChildByClass(QImportFile.class);
        if (childByClass != null) {
            return childByClass.getTextRangeInParent();
        }
        return null;
    }
}