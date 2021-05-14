package org.kdb.inside.brains.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

import java.util.Set;

public interface QVariableElement extends QIdentifier {
    Set<String> IMPLICIT_VARS = Set.of("x", "y", "z");

    @NotNull
    @Override
    String getName();

    /**
     * Returns full name including namespace, if any.
     *
     * @return the full variable name including namespace
     */
    @NotNull
    String getQualifiedName();


    void invalidate();

    /**
     * Checks is the specified variable implicit or not.
     *
     * @param name the variable name to be checked
     * @return <code>true</code> if the variable is in the implicit list; <code>false</code> - otherwise.
     */
    static boolean isImplicitVariable(String name) {
        return IMPLICIT_VARS.contains(name);
    }

    static boolean hasNamespace(String identifier) {
        return !identifier.isEmpty() && identifier.charAt(0) == '.';
    }

    static String createQualifiedName(String namespace, String identifier) {
        if (namespace == null || namespace.isEmpty()) {
            return identifier;
        }
        return namespace + "." + identifier;
    }

    static QVariable createVariable(Project project, String name) {
        return PsiTreeUtil.findChildOfType(QFileType.createFactoryFile(project, name), QVariable.class);
    }
}
