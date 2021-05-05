package org.kdb.inside.brains.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import org.kdb.inside.brains.QFileType;

public interface QSymbolElement extends QIdentifier {
    static QSymbol createSymbol(Project project, String name) {
        if (name.length() == 0 || name.charAt(0) != '`') {
            throw new IllegalArgumentException("Symbol must start with '`' char");
        }
        final QFile file = QFileType.createFactoryFile(project, name);
        return PsiTreeUtil.findChildOfType(file, QSymbol.class);
    }
}
