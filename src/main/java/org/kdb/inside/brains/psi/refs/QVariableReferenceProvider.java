package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.DeclarationRef;

import java.util.*;
import java.util.stream.Stream;

public class QVariableReferenceProvider extends QBaseReferenceProvider<QVariable> {
    private static final EnumSet<ElementScope> IGNORING_DEFINITION_SCOPES = EnumSet.of(ElementScope.DICT, ElementScope.TABLE, ElementScope.QUERY, ElementScope.PARAMETERS);

    public QVariableReferenceProvider() {
        super(QVariable.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QVariable var, @NotNull ProcessingContext context) {
        if (var instanceof QVarReference) {
            return QVariableReference.of(var);
        }

        final ElementScope scope = var.getVariableContext().getScope();
        return IGNORING_DEFINITION_SCOPES.contains(scope) ? PsiReference.EMPTY_ARRAY : QVariableReference.of(var);
    }

    public static class QVariableReference extends QBaseReference<QVariable> {
        public QVariableReference(@NotNull QVariable element) {
            super(element);
        }

        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
            return resolveVariable(myElement, ElementContext.of(myElement));
        }

        public static PsiReference[] of(QVariable var) {
            return new PsiReference[]{new QVariableReference(var)};
        }

        @Override
        protected String getQualifiedName(QVariable element) {
            return element.getQualifiedName();
        }

        private ResolveResult[] resolveVariable(QVariable variable, ElementContext context) {
            if (context.getScope() == ElementScope.QUERY) {
                return resolveQuery(context.query(), variable);
            }

            final QLambdaExpr lambda = variable.getContext(QLambdaExpr.class);
            if (lambda != null) {
                return resolveLambda(lambda, variable);
            }

            return resolveGlobalDefinition(variable);
        }

        private ResolveResult[] resolveQuery(QQueryExpr query, QVariable var) {
            final QExpression expression = query.getSource();
            if (expression == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final ElementContext queryContext = ElementContext.of(query);

            final Collection<QVarReference> refs = PsiTreeUtil.findChildrenOfAnyType(expression, false, QVarReference.class);

            // It's the table definition
            if (var instanceof QVarReference && refs.contains(var)) {
                return resolveVariable(var, queryContext);
            }

            final List<QTableExpr> tables = refs.stream()
                    .map(r -> resolveVariable(r, queryContext))
                    .flatMap(Stream::of)
                    .map(ResolveResult::getElement)
                    .filter(Objects::nonNull)
                    .map(PsiElement::getParent)
                    .filter(e -> e instanceof QAssignmentExpr)
                    .map(e -> ((QAssignmentExpr) e).getExpression())
                    .filter(e -> e instanceof QTableExpr)
                    .map(e -> (QTableExpr) e)
                    .toList();

            final String qualifiedName = var.getQualifiedName();
            List<DeclarationRef> res = new ArrayList<>();
            for (QTableExpr table : tables) {
                Stream.of(table.getKeys(), table.getValues())
                        .filter(Objects::nonNull)
                        .flatMap(v -> v.getColumns().stream())
                        .map(QTableColumn::getVarDeclaration)
                        .filter(Objects::nonNull)
                        .filter(v -> v.getQualifiedName().equals(qualifiedName))
                        .map(DeclarationRef::of)
                        .forEach(res::add);
            }

            if (res.isEmpty()) {
                return resolveVariable(var, queryContext);
            }
            return multi(res);
        }

        private ResolveResult[] resolveLambda(QLambdaExpr lambda, QVariable var) {
            if (QPsiUtil.isImplicitVariable(var) && lambda.getParameters() == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final QVarDeclaration declaration = QPsiUtil.getLocalDefinition(lambda, var);
            if (declaration != null) {
                return single(DeclarationRef.of(declaration));
            }
            return resolveGlobalDefinition(var);
        }
    }
}