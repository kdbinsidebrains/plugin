package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class QVariableReferenceProvider extends QBaseReferenceProvider<QVariable> {
    public QVariableReferenceProvider() {
        super(QVariable.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QVariable var, @NotNull ProcessingContext context) {
        if (var instanceof QVarReference) {
            return QVariableReference.of(var);
        }

        final ElementScope scope = var.getVariableContext().getScope();
        if (scope != ElementScope.PARAMETERS && scope != ElementScope.TABLE && scope != ElementScope.QUERY) {
            return QVariableReference.of(var);
        }

        return PsiReference.EMPTY_ARRAY;
    }

    public static class QVariableReference extends QBaseReference<QVariable> {
        public QVariableReference(@NotNull QVariable element) {
            super(element);
        }

        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
            return resolveVariable(myElement, QPsiUtil.getElementContext(myElement));
        }

        public static PsiReference[] of(QVariable var) {
            return new PsiReference[]{new QVariableReference(var)};
        }

        @Override
        protected String getQualifiedName(QVariable element) {
            return element.getQualifiedName();
        }

        private ResolveResult[] resolveVariable(QVariable variable, ElementContext context) {
            return switch (context.getScope()) {
                case QUERY -> resolveQuery(variable, context.query());
                case LAMBDA -> resolveLambda(variable, context.lambda());
                default -> resolveElement(variable);
            };
        }

        private ResolveResult[] resolveQuery(QVariable var, QQueryExpr query) {
            final QExpression expression = query.getSource();
            if (expression == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final List<QVarReference> refs = new ArrayList<>();
            PsiElement child = expression.getFirstChild();
            while (child != null) {
                if (child instanceof QVarReference) {
                    refs.add((QVarReference) child);
                }
                if (child instanceof LeafPsiElement) {
                    break;
                }
                child = child.getNextSibling();
            }

            final ElementContext queryContext = QPsiUtil.getElementContext(query);

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
            List<QVarDeclaration> res = new ArrayList<>();
            for (QTableExpr table : tables) {
                Stream.of(table.getKeys(), table.getValues())
                        .filter(Objects::nonNull)
                        .flatMap(v -> v.getColumns().stream())
                        .map(QTableColumn::getVarDeclaration)
                        .filter(Objects::nonNull)
                        .filter(v -> v.getQualifiedName().equals(qualifiedName))
                        .forEach(res::add);
            }

            if (res.isEmpty()) {
                return resolveVariable(var, queryContext);
            }
            return multi(res);
        }

        private ResolveResult[] resolveLambda(QVariable var, QLambdaExpr lambda) {
            if (QPsiUtil.isImplicitVariable(var) && lambda.getParameters() == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final String qualifiedName = var.getQualifiedName();
            final Optional<QVarDeclaration> first = findFirstInLambda(qualifiedName, lambda);
            if (first.isPresent()) {
                final QVarDeclaration el = first.get();
                if (QPsiUtil.isGlobalDeclaration(el)) {
                    return resolveElement(var);
                } else {
                    return single(el);
                }
            }
            return resolveElement(var);
        }

        @NotNull
        private Optional<QVarDeclaration> findFirstInLambda(String qualifiedName, QLambdaExpr lambda) {
            return PsiTreeUtil.findChildrenOfType(lambda, QVarDeclaration.class).stream().
                    filter(v -> v.getQualifiedName().equals(qualifiedName)).
                    filter(v -> ElementContext.of(v).any(ElementScope.LAMBDA, ElementScope.PARAMETERS)).
                    findFirst();
        }
    }
}