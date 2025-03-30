package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class QStructureViewElement extends PsiTreeElementBase<PsiElement> {
    private final @NotNull String text;
    private final @NotNull PsiElement navigable;
    private final @NotNull StructureElementType type;
    private final @Nullable Supplier<PsiElement[]> children;

    private final Icon icon;
    private final boolean globalDeclaration;

    protected QStructureViewElement(@NotNull PsiFile file) {
        this(file, StructureElementType.FILE, file.getName(), file, file::getChildren);
    }

    private QStructureViewElement(@NotNull PsiElement element, @NotNull StructureElementType type, @NotNull String text) {
        this(element, type, text, element);
    }

    private QStructureViewElement(@NotNull PsiElement element, @NotNull StructureElementType type, @NotNull String text, @Nullable PsiElement navigable) {
        this(element, type, text, navigable, null);
    }

    private QStructureViewElement(@NotNull PsiElement element, @NotNull StructureElementType type, @NotNull String text, @Nullable PsiElement navigable, @Nullable Supplier<PsiElement[]> children) {
        super(element);
        this.type = type;
        this.text = text;
        this.children = children;
        this.navigable = navigable == null ? element : navigable;

        if (navigable instanceof QVarDeclaration d && !(element instanceof QContext)) {
            icon = type.getIcon(globalDeclaration = QPsiUtil.isGlobalDeclaration(d));
        } else if (navigable instanceof QSymbol) {
            icon = type.getIcon(globalDeclaration = true);
        } else {
            globalDeclaration = true;
            icon = type.getIcon();
        }
    }

    @Override
    public Icon getIcon(boolean open) {
        return icon;
    }

    @Override
    public @NotNull String getPresentableText() {
        return text;
    }

    public @NotNull StructureElementType getType() {
        return type;
    }

    @Override
    public boolean canNavigate() {
        return navigable instanceof Navigatable n && n.canNavigate();
    }

    public boolean isGlobalDeclaration() {
        return globalDeclaration;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (navigable instanceof Navigatable n) {
            n.navigate(requestFocus);
        }
    }

    @Override
    public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
        if (children == null || type.isAlwaysLeaf()) {
            return List.of();
        }
        final PsiElement[] values = children.get();
        if (values == null) {
            return List.of();
        }
        return Stream.of(values).flatMap(QStructureViewElement::createViewElement).filter(Objects::nonNull).toList();
    }

    public static @NotNull Stream<StructureViewTreeElement> createViewElement(@NotNull PsiElement element) {
        if (element instanceof QImport i) {
            return Stream.ofNullable(createImport(i));
        }
        if (element instanceof QCommand cmd) {
            return Stream.ofNullable(createCommand(cmd));
        }
        if (element instanceof QContext context) {
            return Stream.ofNullable(createContext(context));
        }
        if (element instanceof QTableColumn col) {
            return Stream.ofNullable(createTableColumns(col));
        }
        if (element instanceof QInvokeFunction func) {
            return Stream.ofNullable(createSet(func));
        }
        if (element instanceof QAssignmentExpr assignment) {
            return createAssignment(assignment);
        }
        return Stream.empty();
    }

    private static @NotNull QStructureViewElement createImport(QImport qImport) {
        return new QStructureViewElement(qImport, StructureElementType.IMPORT, qImport.getFilePath());
    }

    private static @NotNull QStructureViewElement createCommand(QCommand cmd) {
        return new QStructureViewElement(cmd, StructureElementType.COMMAND, cmd.getText());
    }

    private static @NotNull QStructureViewElement createContext(QContext context) {
        final QVarDeclaration nameVar = context.getVariable();
        final String name = nameVar == null ? "." : nameVar.getName();
        return new QStructureViewElement(context, StructureElementType.CONTEXT, name, nameVar, () -> {
            final QContextBody body = context.getContextBody();
            return body == null ? null : body.getChildren();
        });
    }

    private static @NotNull Stream<StructureViewTreeElement> createAssignment(QAssignmentExpr assignment) {
        return assignment.getVarAssignments().stream().map(QStructureViewElement::createVarAssignment);
    }

    private static @NotNull QStructureViewElement createVarAssignment(VarAssignment assignment) {
        final QVarDeclaration variable = assignment.declaration();
        final QExpression expression = assignment.expression();

        final String name = variable.getQualifiedName();
        if (expression instanceof QLambdaExpr lambda) {
            return new QStructureViewElement(lambda, StructureElementType.LAMBDA, name + lambda.getParametersInfo(), variable, () -> {
                final QExpressions expressions = lambda.getExpressions();
                return expressions == null ? null : expressions.getExpressionList().toArray(PsiElement[]::new);
            });
        }
        if (expression instanceof QTableExpr tbl) {
            return new QStructureViewElement(tbl, StructureElementType.TABLE, name, variable, () -> tbl.getColumns().toArray(PsiElement[]::new));
        }
        if (expression instanceof QDictExpr dict) {
            return new QStructureViewElement(dict, StructureElementType.DICT, name, variable, () -> dict.getColumns().toArray(PsiElement[]::new));
        }
        return new QStructureViewElement(variable, StructureElementType.VARIABLE, name + ": " + getExpressionType(expression));
    }

    private static @NotNull QStructureViewElement createTableColumns(QTableColumn col) {
        final PsiElement parent = col.getParent();
        final boolean keys = parent instanceof QTableKeys;
        final QVarDeclaration varDeclaration = col.getVarDeclaration();

        final QExpression expression = col.getExpression();
        final String desc = expression == null ? "reference" : getExpressionType(expression);
        final String name = varDeclaration == null ? desc : varDeclaration.getQualifiedName() + ": " + desc;
        final StructureElementType elType = keys ? (parent.getParent() instanceof QDictExpr ? StructureElementType.DICT_FIELD : StructureElementType.TABLE_KEY_COLUMN) : StructureElementType.TABLE_VALUE_COLUMN;
        return new QStructureViewElement(col, elType, name);
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
        final String name = sym.getName() + (expressions.size() == 2 ? ": " + getExpressionType(expressions.get(1)) : "");
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
        final String name = sym.getName() + ": " + getExpressionType(ff.getExpression());
        return new QStructureViewElement(sym, StructureElementType.SYMBOL, name);
    }

    private static @NotNull String getExpressionType(QExpression expression) {
        if (expression == null) {
            return "";
        }
        if (expression instanceof QTypeCastExpr tc) {
            String t = tc.getTypeCast().getText();
            return t.substring(t.charAt(0) == '`' ? 1 : 0, t.length() - 1);
        }
        if (expression instanceof QVarReference ref) {
            return ref.getQualifiedName();
        }
        if (expression instanceof QLiteralExpr l) {
            if (l.getSymbol() != null) {
                return "symbol";
            }
            if (l.getSymbols() != null) {
                return "symbols";
            }
            return l.getFirstChild().getNode().getElementType().toString();
        }
        if (expression instanceof QQueryExpr) {
            return "query";
        }
        return "expression";
    }
}
