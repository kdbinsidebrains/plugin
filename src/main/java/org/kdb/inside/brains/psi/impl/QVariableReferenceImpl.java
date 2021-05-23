package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import org.kdb.inside.brains.psi.QVarReference;

public class QVariableReferenceImpl extends QVariableBase implements QVarReference {
    public QVariableReferenceImpl(ASTNode node) {
        super(node);
    }
}
