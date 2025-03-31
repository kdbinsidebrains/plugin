package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import org.kdb.inside.brains.psi.QVarReference;

public class QVariableReferenceMixin extends QVariableBase implements QVarReference {
    public QVariableReferenceMixin(ASTNode node) {
        super(node);
    }
}
