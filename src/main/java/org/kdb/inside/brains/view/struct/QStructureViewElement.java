package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QStructureViewElement extends PsiTreeElementBase<PsiElement> {
    private final String text;
    private final PsiElement content;
    private final StructureElementType type;


    protected QStructureViewElement(PsiFile file) {
        this(file, StructureElementType.FILE, file.getName(), file);
    }

    private QStructureViewElement(PsiElement element, StructureElementType type, String text) {
        this(element, type, text, null);
    }

    private QStructureViewElement(PsiElement element, StructureElementType type, String text, PsiElement content) {
        super(element);
        this.type = type;
        this.text = text;
        this.content = content;
    }

    @Override
    public Icon getIcon(boolean open) {
        return type.getIcon();
    }

    @Override
    public @Nullable String getPresentableText() {
        return text;
    }

    public StructureElementType getType() {
        return type;
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (content == null || type == StructureElementType.LOAD || type == StructureElementType.COMMAND || type == StructureElementType.VARIABLE) {
            return List.of();
        }

        if (content instanceof QTableExpr) {
            return getTableElements((QTableExpr) content);
        }
        return processChildren(content);
    }

    private @NotNull Collection<StructureViewTreeElement> processChildren(PsiElement content) {
        return Stream.of(content.getChildren()).map(this::createChildElement).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private @Nullable QStructureViewElement createChildElement(PsiElement child) {
        if (child instanceof QImport) {
            final QImport qImport = (QImport) child;
            return new QStructureViewElement(child, StructureElementType.LOAD, qImport.getFilePath());
        } else if (child instanceof QCommand) {
            return new QStructureViewElement(child, StructureElementType.COMMAND, child.getText());
        } else if (child instanceof QContext) {
            final QContext context = (QContext) child;
            final QContextBody body = context.getContextBody();
            final QVarDeclaration nameVar = context.getVariable();
            if (nameVar != null) {
                return new QStructureViewElement(nameVar, StructureElementType.CONTEXT, nameVar.getName(), body);
            } else {
                return new QStructureViewElement(context, StructureElementType.CONTEXT, ".", body);
            }
        } else if (child instanceof QAssignmentExpr) {
            final QAssignmentExpr assignment = (QAssignmentExpr) child;
            final QVarDeclaration variable = assignment.getVarDeclaration();
            if (variable == null || !QPsiUtil.isGlobalDeclaration(variable)) {
                return null;
            }

            final QExpression expression = assignment.getExpression();
            if (expression == null) {
                return null;
            }

            String name = variable.getQualifiedName();
            if (expression instanceof QLambdaExpr) {
                final QLambdaExpr lambda = (QLambdaExpr) expression;
                final QParameters parameters = lambda.getParameters();
                if (parameters == null) {
                    name += "[]";
                } else {
                    final String collect = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
                    name += "[" + collect + "]";
                }
                return new QStructureViewElement(variable, StructureElementType.LAMBDA, name, lambda.getExpressions());
            } else if (expression instanceof QTableExpr) {
                return new QStructureViewElement(variable, StructureElementType.TABLE, name, expression);
            } else {
                name += ": " + getVariableType(expression);
                return new QStructureViewElement(variable, StructureElementType.VARIABLE, name);
            }
        }
        return null;
    }

    private String getVariableType(QExpression expression) {
        if (expression instanceof QTypeCastExpr) {
            return QPsiUtil.getTypeCast((QTypeCastExpr) expression);
        }

        if (expression instanceof QLiteralExpr) {
            return "literal";
        }
        if (expression instanceof QQueryExpr) {
            return "query";
        }
        return "expression";
    }

    private @NotNull Collection<StructureViewTreeElement> getTableElements(QTableExpr tbl) {
        return Stream.concat(
                collectColumns(tbl.getKeys(), StructureElementType.TABLE_KEY_COLUMN),
                collectColumns(tbl.getValues(), StructureElementType.TABLE_VALUE_COLUMN)
        ).collect(Collectors.toList());
    }

    @NotNull
    private Stream<StructureViewTreeElement> collectColumns(QTableColumns columns, StructureElementType type) {
        return Stream.of(columns)
                .filter(Objects::nonNull)
                .flatMap(v -> v.getColumns().stream())
                .filter(v -> v.getVarDeclaration() != null)
                .map(v -> new QStructureViewElement(v.getVarDeclaration(), type, v.getVarDeclaration().getName()));
    }
}
