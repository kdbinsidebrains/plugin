package org.kdb.inside.brains.lang.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import org.kdb.inside.brains.psi.QPsiElement;
import org.kdb.inside.brains.psi.QVarDeclaration;

public class QInplaceVariableIntroducer extends InplaceVariableIntroducer<QPsiElement> {
    public static final QPsiElement[] EMPTY_OCCURRENCES = new QPsiElement[0];

    public QInplaceVariableIntroducer(QVarDeclaration variable, Editor editor, Project project) {
        super(variable, editor, project, "Introduce Variable", EMPTY_OCCURRENCES, null);
    }
}
