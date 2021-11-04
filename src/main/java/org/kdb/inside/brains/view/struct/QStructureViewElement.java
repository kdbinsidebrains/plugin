package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QCommand;
import org.kdb.inside.brains.psi.QImport;
import org.kdb.inside.brains.psi.QTableColumns;
import org.kdb.inside.brains.psi.QTableExpr;

import javax.swing.*;
import java.util.ArrayList;
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
/*

    @Deprecated
    protected QStructureViewElement(PsiElement reference, StructureElementType type) {
        this(reference, reference, type);
    }

    @Deprecated
    protected QStructureViewElement(PsiElement reference, PsiElement content, StructureElementType type) {
        super(reference);
        this.type = type;
        this.content = content;
    }
*/


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
/*
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
        if (content instanceof QContextBody) {
            final QVarDeclaration variable = ((QContext) content.getParent()).getVariable();
            return variable != null ? variable.getText() : "\\d";
        }
        if (content instanceof QTableExpr || content instanceof QVariable) {
            return ((QVariable) element).getQualifiedName();
        }
        if (content instanceof QLambdaExpr) {
            final QParameters parameters = ((QLambdaExpr) content).getParameters();
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
    }*/
/*

    private String getVariableType(QExpression expression) {
        final PsiElement firstChild = expression.getFirstChild();
        final PsiElement lastChild = expression.getLastChild();

        if (firstChild instanceof QTypeCastExpr) {
            return QPsiUtil.getTypeCast((QTypeCastExpr) firstChild);
        }

        if (firstChild == lastChild) {
            if (firstChild instanceof QLiteralExpr) {
                return "atom";
            }
            if (firstChild instanceof QQueryExpr) {
                return "query";
            }
            if (firstChild instanceof QInvokeExpr) {
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
*/

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (content == null || type == StructureElementType.LOAD || type == StructureElementType.COMMAND || type == StructureElementType.VARIABLE) {
            return List.of();
        }

        if (content instanceof QTableExpr) {
            return getTableElements((QTableExpr) content);
        }

        return processChildren(content);

/*
        if (type == StructureElementType.TABLE) {
            return getTableElements((QTableExpr) content);
        } else {
            Collection<StructureViewTreeElement> res = new ArrayList<>();
            for (PsiElement child : content.getChildren()) {
                if (child instanceof QImport) {
                    res.add(new QStructureViewElement(child, StructureElementType.LOAD));
                } else if (child instanceof QCommand) {
                    res.add(new QStructureViewElement(child, StructureElementType.COMMAND));
                } else if (child instanceof QContext) {
                    final QContext context = (QContext) child;
                    res.add(new QStructureViewElement(context.getVariable(), context.getContextBody(), StructureElementType.CONTEXT));
                } else if (child instanceof QAssignmentExpr) {
                    final QAssignmentExpr assignment = (QAssignmentExpr) child;
                    final QVarDeclaration variable = assignment.getVarDeclaration();
                    if (variable == null || !QPsiUtil.isGlobalDeclaration(variable)) {
                        continue;
                    }

                    final QExpression expression = assignment.getExpression();
                    if (expression == null) {
                        continue;
                    }

                    if (expression instanceof QLambdaExpr) {
                        res.add(new QStructureViewElement(variable, ((QLambdaExpr) expression).getExpressions(), StructureElementType.LAMBDA));
                    } else if (expression instanceof QTableExpr) {
                        res.add(new QStructureViewElement(variable, expression, StructureElementType.TABLE));
                    } else {
                        res.add(new QStructureViewElement(variable, expression, StructureElementType.VARIABLE));
                    }
                }
            }
            return res;
        }
*/
    }

    @NotNull
    private Collection<StructureViewTreeElement> processChildren(PsiElement content) {
        final Collection<StructureViewTreeElement> res = new ArrayList<>();
        for (PsiElement child : content.getChildren()) {
            if (child instanceof QImport) {
                final QImport qImport = (QImport) child;
                res.add(new QStructureViewElement(child, StructureElementType.LOAD, qImport.getFilePath()));
            } else if (child instanceof QCommand) {
//                res.add(new QStructureViewElement(child, StructureElementType.COMMAND));
            }
        }
        return res;
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
