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

import static org.kdb.inside.brains.psi.QPsiUtil.getImportContent;

public class QStructureViewElement extends PsiTreeElementBase<PsiElement> {
    private final String text;
    private final PsiElement content;
    private final @NotNull StructureElementType type;

    protected QStructureViewElement(@NotNull PsiFile file) {
        this(file, StructureElementType.FILE, file.getName(), file);
    }

    private QStructureViewElement(@NotNull PsiElement element, @NotNull StructureElementType type, String text) {
        this(element, type, text, null);
    }

    private QStructureViewElement(@NotNull PsiElement element, @NotNull StructureElementType type, String text, PsiElement content) {
        super(element);
        this.type = type;
        this.text = text;
        this.content = content;
    }

    @Override
    public @Nullable String getPresentableText() {
        return text;
    }

    public @NotNull StructureElementType getType() {
        return type;
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (content == null || type.isAlwaysLeaf()) {
            return List.of();
        }
        if (content instanceof QDictExpr d) {
            return getDictChildren(d);
        }
        if (content instanceof QTableExpr t) {
            return getTableChildren(t);
        }
        return Stream.of(content.getChildren()).map(QStructureViewElement::createViewElement).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private @NotNull Collection<StructureViewTreeElement> getDictChildren(QDictExpr d) {
        final List<QTableColumn> fields = d.getFields();
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .map(QStructureViewElement::createViewElement)
                .collect(Collectors.toList());
    }

    private @NotNull Collection<StructureViewTreeElement> getTableChildren(QTableExpr t) {
        return Stream.of(t.getKeys(), t.getValues())
                .filter(Objects::nonNull)
                .map(QTableColumns::getColumns)
                .flatMap(Collection::stream)
                .map(QStructureViewElement::createViewElement)
                .collect(Collectors.toList());
    }

    public static @Nullable QStructureViewElement createViewElement(PsiElement child) {
        if (child instanceof QImport qImport) {
            return createImport(qImport);
        } else if (child instanceof QCommand cmd) {
            return createCommand(cmd);
        } else if (child instanceof QContext context) {
            return createContext(context);
        } else if (child instanceof QLambdaExpr lambda) {
            return createLambdaElement(lambda, "\uD835\uDF06");
        } else if (child instanceof QTableColumn col) {
            return createTableColumns(col);
        } else if (child instanceof QAssignmentExpr assignment) {
            return createAssignment(assignment);
        } else if (child instanceof QInvokeFunction func) {
            return createSet(func);
        }
        return null;
    }

    private static @Nullable QStructureViewElement createSet(QInvokeFunction f) {
        final QSystemFunction systemFunction = f.getSystemFunction();
        if (systemFunction != null) {
            return createSystemSet(f, systemFunction);
        }

        final QCustomFunction cf = f.getCustomFunction();
        if (cf != null) {
            return createCustomSet(f, cf);
        }
        return null;
    }

    // set[`adfadf;...]
    private static @Nullable QStructureViewElement createSystemSet(QInvokeFunction f, QSystemFunction systemFunction) {
        if (!"set".equals(systemFunction.getText())) {
            return null;
        }
        final List<QArguments> argumentsList = f.getArgumentsList();
        if (argumentsList.isEmpty()) {
            return null;
        }
        final List<QExpression> expressions = argumentsList.get(0).getExpressions();
        if (expressions.isEmpty()) {
            return null;
        }

        final PsiElement firstChild = expressions.get(0).getFirstChild();
        if (!(firstChild instanceof QSymbol sym)) {
            return null;
        }
        final String name = sym.getName() + (expressions.size() == 2 ? getExpressionType(expressions.get(1)) : "");
        return new QStructureViewElement(sym, StructureElementType.SYMBOL, name);
    }

    // `asdasd set ...
    private static @Nullable QStructureViewElement createCustomSet(QInvokeFunction f, QCustomFunction cf) {
        final PsiElement firstChild = cf.getExpression().getFirstChild();
        if (!(firstChild instanceof QSymbol sym)) {
            return null;
        }
        final QExpression expression = f.getExpression();
        if (!(expression instanceof QInvokeFunction ff)) {
            return null;
        }
        final QSystemFunction sf = ff.getSystemFunction();
        if (sf == null || !"set".equals(sf.getText())) {
            return null;
        }
        final String name = sym.getName() + getExpressionType(ff.getExpression());
        return new QStructureViewElement(sym, StructureElementType.SYMBOL, name);
    }

    private static @Nullable QStructureViewElement createAssignment(QAssignmentExpr assignment) {
        final QVarDeclaration variable = assignment.getVarDeclaration();
        if (variable == null) {
            return null;
        }

        final QExpression expression = assignment.getExpression();
        if (expression == null) {
            return null;
        }

        String name = variable.getQualifiedName();
        if (expression instanceof QLambdaExpr lambda) {
            return createLambdaElement(lambda, name);
        } else if (expression instanceof QTableExpr) {
            return new QStructureViewElement(assignment, StructureElementType.TABLE, name, expression);
        } else if (expression instanceof QDictExpr) {
            return new QStructureViewElement(assignment, StructureElementType.DICT, name, expression);
        } else {
            name += getExpressionType(expression);
            return new QStructureViewElement(assignment, StructureElementType.VARIABLE, name);
        }
    }

    private static @NotNull QStructureViewElement createTableColumns(QTableColumn col) {
        final PsiElement parent = col.getParent();
        final boolean keys = parent instanceof QTableKeys;
        final QVarDeclaration varDeclaration = col.getVarDeclaration();

        String name = varDeclaration == null ? "" : varDeclaration.getQualifiedName();
        name += getExpressionType(col.getExpression());

        final StructureElementType elType = keys ? (parent.getParent() instanceof QDictExpr ? StructureElementType.DICT_FIELD : StructureElementType.TABLE_KEY_COLUMN) : StructureElementType.TABLE_VALUE_COLUMN;
        return new QStructureViewElement(col, elType, name);
    }

    private static @NotNull QStructureViewElement createCommand(QCommand cmd) {
        return new QStructureViewElement(cmd, StructureElementType.COMMAND, cmd.getText());
    }

    private static @NotNull QStructureViewElement createImport(QImport qImport) {
        return new QStructureViewElement(qImport, StructureElementType.IMPORT, getImportContent(qImport));
    }

    private static @NotNull QStructureViewElement createContext(QContext context) {
        final QContextBody body = context.getContextBody();
        final QVarDeclaration nameVar = context.getVariable();
        if (nameVar == null) {
            return new QStructureViewElement(context, StructureElementType.CONTEXT, ".", body);
        } else {
            return new QStructureViewElement(nameVar, StructureElementType.CONTEXT, nameVar.getName(), body);
        }
    }

    @NotNull
    private static QStructureViewElement createLambdaElement(QLambdaExpr lambda, String namePrefix) {
        return new QStructureViewElement(lambda, StructureElementType.LAMBDA, namePrefix + lambda.getParametersInfo(), lambda.getExpressions());
    }

    private static String getExpressionType(QExpression expression) {
        if (expression == null) {
            return "";
        }
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
}
