package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

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

    public static @Nullable QStructureViewElement createViewElement(PsiElement child) {
        if (child instanceof QImport qImport) {
            return new QStructureViewElement(child, StructureElementType.IMPORT, qImport.getFilePath());
        } else if (child instanceof QCommand) {
            return new QStructureViewElement(child, StructureElementType.COMMAND, child.getText());
        } else if (child instanceof QContext context) {
            final QContextBody body = context.getContextBody();
            final QVarDeclaration nameVar = context.getVariable();
            if (nameVar != null) {
                return new QStructureViewElement(child, StructureElementType.CONTEXT, nameVar.getName(), body);
            } else {
                return new QStructureViewElement(child, StructureElementType.CONTEXT, ".", body);
            }
        } else if (child instanceof QLambdaExpr) {
            return createLambdaElement(child, (QLambdaExpr) child, "\uD835\uDF06");
        } else if (child instanceof QTableColumn col) {
            final boolean keys = col.getParent() instanceof QTableKeys;
            final QVarDeclaration varDeclaration = col.getVarDeclaration();

            String name = varDeclaration == null ? "" : varDeclaration.getQualifiedName();
            name += getExpressionType(col.getExpression());
            return new QStructureViewElement(child, keys ? StructureElementType.TABLE_KEY_COLUMN : StructureElementType.TABLE_VALUE_COLUMN, name);
        } else if (child instanceof QAssignmentExpr assignment) {
            final QVarDeclaration variable = assignment.getVarDeclaration();
            if (variable == null) {
                return null;
            }

            final QExpression expression = assignment.getExpression();
            if (expression == null) {
                return null;
            }

            String name = variable.getQualifiedName();
            if (expression instanceof QLambdaExpr) {
                return createLambdaElement(child, (QLambdaExpr) expression, name);
            } else if (expression instanceof QTableExpr) {
                return new QStructureViewElement(child, StructureElementType.TABLE, name, expression);
            } else {
                name += getExpressionType(expression);
                return new QStructureViewElement(child, StructureElementType.VARIABLE, name);
            }
        }
        return null;
    }

    @NotNull
    private static QStructureViewElement createLambdaElement(PsiElement element, QLambdaExpr lambda, String namePrefix) {
        final QParameters parameters = lambda.getParameters();
        if (parameters == null) {
            namePrefix += "[]";
        } else {
            final String collect = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
            namePrefix += "[" + collect + "]";
        }
        return new QStructureViewElement(element, StructureElementType.LAMBDA, namePrefix, lambda.getExpressions());
    }

    private static String getExpressionType(QExpression expression) {
        if (expression instanceof QTypeCastExpr) {
            return ": " + QPsiUtil.getTypeCast((QTypeCastExpr) expression);
        }
        if (expression instanceof QLiteralExpr) {
            return ": " + expression.getText();
        }
        if (expression instanceof QQueryExpr) {
            return ": query";
        }
        return ": expression";
    }

    @Override
    public @Nullable String getPresentableText() {
        return text;
    }

    private @NotNull Collection<StructureViewTreeElement> processChildren(PsiElement content) {
        return Stream.of(content.getChildren()).map(this::createChildElement).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public StructureElementType getType() {
        return type;
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (content == null || type.isAlwaysLeaf()) {
            return List.of();
        }

        if (content instanceof QTableExpr) {
            return getTableElements((QTableExpr) content);
        }
        return processChildren(content);
    }

    private @Nullable QStructureViewElement createChildElement(PsiElement child) {
        return createViewElement(child);
    }

    private @NotNull Collection<StructureViewTreeElement> getTableElements(QTableExpr tbl) {
        return Stream.of(tbl.getKeys(), tbl.getValues())
                .filter(Objects::nonNull)
                .map(QTableColumns::getColumns)
                .flatMap(Collection::stream)
                .map(QStructureViewElement::createViewElement)
                .collect(Collectors.toList());
    }
}
