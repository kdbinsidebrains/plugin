package org.kdb.inside.brains.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.QLanguage;

public final class QFile extends PsiFileBase {
    public QFile(FileViewProvider viewProvider) {
        super(viewProvider, QLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return QFileType.INSTANCE;
    }
}
