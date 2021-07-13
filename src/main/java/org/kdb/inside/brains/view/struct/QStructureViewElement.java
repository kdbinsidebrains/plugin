package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class QStructureViewElement extends PsiTreeElementBase<PsiElement> {
    private final PsiElement content;
    private final StructureElementType type;

    protected QStructureViewElement(PsiFile file) {
        this(file, StructureElementType.FILE);
    }

    protected QStructureViewElement(PsiElement reference, StructureElementType type) {
        this(reference, reference, type);
    }

    protected QStructureViewElement(PsiElement reference, PsiElement content, StructureElementType type) {
        super(reference);
        this.type = type;
        this.content = content;
    }

    public StructureElementType getType() {
        return type;
    }

    @Override
    public Icon getIcon(boolean open) {
        return type.getIcon();
    }

    @Override
    public String getPresentableText() {
        final PsiElement element = getElement();
        if (content == null || element == null) {
            return null;
        }

        if (content instanceof QFile) {
            return ((QFile) content).getName();
        }
        if (content instanceof QImport) {
            return content.getText();
        }
        if (content instanceof QCommand) {
            return content.getText();
        }
        if (content instanceof QContext) {
            final QVarDeclaration variable = ((QContext) content).getVariable();
            return variable != null ? variable.getText() : "\\d";
        }
        if (content instanceof QTable || content instanceof QVariable) {
            return ((QVariable) element).getQualifiedName();
        }
        if (content instanceof QLambda) {
            final QParameters parameters = ((QLambda) content).getParameters();
            if (parameters == null) {
                return ((QVariable) element).getQualifiedName() + "[]";
            }

            final String collect = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
            return ((QVariable) element).getQualifiedName() + "[" + collect + "]";
        }
        if (content instanceof QExpression) {
            return ((QVariable) element).getQualifiedName() + ": " + getVariableType((QExpression) content);
        }

        return element.getClass().getSimpleName();
    }

    private String getVariableType(QExpression expression) {
        final PsiElement firstChild = expression.getFirstChild();
        final PsiElement lastChild = expression.getLastChild();

        if (firstChild instanceof QTypeCast && lastChild instanceof QList) {
            return QPsiUtil.getTypeCast((QTypeCast) firstChild);
        }

        if (firstChild == lastChild) {
            if (firstChild instanceof QList) {
                return "list";
            }
            if (firstChild instanceof QQuery) {
                return "query";
            }
            if (firstChild instanceof QInvoke) {
                return "invoke";
            }
            if (firstChild instanceof LeafPsiElement) {
                return ((LeafPsiElement) firstChild).getElementType().toString();
            }
            if (firstChild instanceof QPsiElement) {
                return firstChild.getClass().getSimpleName().toLowerCase();
            }
            return "undefined";
        }
        return "expression";
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (type == StructureElementType.LOAD || type == StructureElementType.COMMAND || type == StructureElementType.VARIABLE) {
            return List.of();
        }

        if (type == StructureElementType.TABLE) {
            return getTableElements((QTable) content);
        } else {
            Collection<StructureViewTreeElement> res = new ArrayList<>();
            for (PsiElement child : content.getChildren()) {
                if (child instanceof QImport) {
                    res.add(new QStructureViewElement(child, StructureElementType.LOAD));
                } else if (child instanceof QCommand) {
                    res.add(new QStructureViewElement(child, StructureElementType.COMMAND));
                } else if (child instanceof QContext) {
                    res.add(new QStructureViewElement(child, StructureElementType.CONTEXT));
                } else if (child instanceof QExpression) {
                    final QExpression ex = (QExpression) child;
                    final List<QVariableAssignment> assignments = ex.getVariableAssignmentList();
                    for (QVariableAssignment assignment : assignments) {
                        final QVarDeclaration variable = assignment.getVariable();
                        if (!QPsiUtil.isGlobalDeclaration(variable)) {
                            continue;
                        }

                        final QExpression expression = assignment.getExpression();
                        if (expression == null) {
                            continue;
                        }

                        if (!expression.getLambdaList().isEmpty()) {
                            res.add(new QStructureViewElement(variable, expression.getLambdaList().get(0), StructureElementType.LAMBDA));
                        } else if (!expression.getTableList().isEmpty()) {
                            res.add(new QStructureViewElement(variable, expression.getTableList().get(0), StructureElementType.TABLE));
                        } else {
                            res.add(new QStructureViewElement(variable, expression, StructureElementType.VARIABLE));
                        }
                    }
                }
            }
            return res;
        }
    }

    private @NotNull Collection<StructureViewTreeElement> getTableElements(QTable tbl) {
        final QKeyColumns keys = tbl.getKeyColumns();
        final QValueColumns values = tbl.getValueColumns();

        Collection<StructureViewTreeElement> res = new ArrayList<>();
        if (keys != null) {
            for (QTableColumn column : keys.getColumns()) {
                res.add(new QStructureViewElement(column.getVariable(), column.getExpression(), StructureElementType.TABLE_KEY_COLUMN));
            }
        }
        if (values != null) {
            for (QTableColumn column : values.getColumns()) {
                res.add(new QStructureViewElement(column.getVariable(), column.getExpression(), StructureElementType.TABLE_VALUE_COLUMN));
            }
        }
        return res;
    }
}
