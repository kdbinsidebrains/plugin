package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.openapi.util.LastComputedIconCache;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.IconManager;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.VisibilityIcons;
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

    public static @Nullable QStructureViewElement createViewElement(PsiElement child) {
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
                return new QStructureViewElement(child, StructureElementType.CONTEXT, nameVar.getName(), body);
            } else {
                return new QStructureViewElement(child, StructureElementType.CONTEXT, ".", body);
            }
        } else if (child instanceof QTableColumn) {
            final QTableColumn col = (QTableColumn) child;
            final boolean keys = col.getParent() instanceof QTableKeys;
            final QVarDeclaration varDeclaration = col.getVarDeclaration();
            final String name = varDeclaration == null ? "" : varDeclaration.getQualifiedName();
            return new QStructureViewElement(child, keys ? StructureElementType.TABLE_KEY_COLUMN : StructureElementType.TABLE_VALUE_COLUMN, name);
        } else if (child instanceof QAssignmentExpr) {
            final QAssignmentExpr assignment = (QAssignmentExpr) child;
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
                final QLambdaExpr lambda = (QLambdaExpr) expression;
                final QParameters parameters = lambda.getParameters();
                if (parameters == null) {
                    name += "[]";
                } else {
                    final String collect = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
                    name += "[" + collect + "]";
                }
                return new QStructureViewElement(child, StructureElementType.LAMBDA, name, lambda.getExpressions());
            } else if (expression instanceof QTableExpr) {
                return new QStructureViewElement(child, StructureElementType.TABLE, name, expression);
            } else {
                name += ": " + getExpressionType(expression);
                return new QStructureViewElement(child, StructureElementType.VARIABLE, name);
            }
        }
        return null;
    }

    private static String getExpressionType(QExpression expression) {
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

    @Override
    public Icon getIcon(boolean open) {
        final PsiElement element = getElement();
        if (element == null) {
            return null;
        }

        Icon base = LastComputedIconCache.get(element, 0);
        if (base == null) {
            final Icon icon = getBaseIcon();
            if (element instanceof QAssignmentExpr) {
                final QAssignmentExpr assignmentExpr = (QAssignmentExpr) element;
                final RowIcon baseIcon = IconManager.getInstance().createLayeredIcon(element, icon, 0);
                if (QPsiUtil.isGlobalDeclaration(assignmentExpr)) {
                    VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PUBLIC, baseIcon);
                } else {
                    VisibilityIcons.setVisibilityIcon(PsiUtil.ACCESS_LEVEL_PRIVATE, baseIcon);
                }
                base = baseIcon;
            } else {
                base = icon;
            }
        }
        return base;
    }

    @Override
    public @Nullable String getPresentableText() {
        return text;
    }

    public Icon getBaseIcon() {
        return type.getIcon();
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
