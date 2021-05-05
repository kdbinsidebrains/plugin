package org.kdb.inside.brains.psi;

import com.intellij.psi.tree.IElementType;
import org.kdb.inside.brains.QLanguage;

public final class QElementType extends IElementType {
  public QElementType(String debugName) {
    super(debugName, QLanguage.INSTANCE);
  }
}
