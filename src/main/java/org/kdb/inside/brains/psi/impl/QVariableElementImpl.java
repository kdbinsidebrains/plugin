package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QPsiUtil;
import org.kdb.inside.brains.psi.*;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

public abstract class QVariableElementImpl extends QIdentifierImpl implements QVariableElement {
    private String name = null;
    private String qualifiedName = null;


    QVariableElementImpl(ASTNode node) {
        super(node);
    }

    @NotNull
    public String getName() {
        if (name == null) {
            name = getText();
            if (name.endsWith("IntellijIdeaRulezzz")) {
                name = name.substring(0, name.length() - 19);
            }
        }
        return name;
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        if (qualifiedName == null) {
            qualifiedName = calculateQualifiedName();
        }
        return qualifiedName;
    }

    @NotNull
    private String calculateQualifiedName() {
        final String name = getName();

        // if has namespace - it's global in any case
        if (QVariableElement.hasNamespace(name)) {
            return name;
        }

        // It's namespace name itself - ignore
        if (getParent() instanceof QContext) {
            return name;
        }

        // No namespace - ignore
        final QContext context = PsiTreeUtil.getParentOfType(this, QContext.class);
        if (context == null || context.getVariable() == null) {
            return name;
        }

        // root namespace - ignore
        final String namespaceName = context.getVariable().getName();
        if (".".equals(namespaceName)) {
            return name;
        }

        // no lambda - return full name
        final QLambda lambda = getContext(QLambda.class);
        if (lambda != null) {
            final QParameters lambdaParams = lambda.getParameters();
            // implicit variable or in parameters list - ignore namespace
            if (lambdaParams == null) {
                if (QVariableElement.isImplicitVariable(name)) {
                    return name;
                }
            } else {
                final List<QVariable> variables = lambdaParams.getVariableList();
                if (variables.contains(this)) {
                    return name;
                }
            }
        }
        return QVariableElement.generateQualifiedName(namespaceName, name);
    }

    public boolean isDeclaration() {
        final PsiElement parent = getParent();
        if (parent instanceof QParameters) {
            return true;
        }
        if (parent instanceof QVariableAssignment) {
            return ((QVariableAssignment) parent).getArguments() == null;
        }
        return false;
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        invalidate();
    }

    // Indicates the scope of files in which to find usages of this PsiElement
    // For find usages, this should include all files in the top level project
    @NotNull
    @Override
    public SearchScope getUseScope() {
        return GlobalSearchScope.projectScope(getProject());
    }

    public PsiElement setName(@NotNull String newName) {
        Optional.ofNullable(QVariableElement.createVariable(getProject(), newName))
                .map(QVariable::getFirstChild)
                .map(PsiElement::getNode)
                .ifPresent(newKeyNode -> {
                    final ASTNode keyNode = getNode().getFirstChildNode();
                    getNode().replaceChild(keyNode, newKeyNode);
                });
        return this;
    }

    @Override
    public boolean isEquivalentTo(PsiElement object) {
        if (object instanceof QVariableElement) {
            final QVariableElement obj = (QVariableElement) object;
            return obj.getQualifiedName().equals(getQualifiedName());
        }
        return false;
    }

    @Override
    public void invalidate() {
        name = null;
        qualifiedName = null;
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @NotNull
            @Override
            public String getPresentableText() {
                return getQualifiedName();
            }

            @NotNull
            @Override
            public String getLocationString() {
                final PsiFile containingFile = getContainingFile();
                return containingFile == null ? "" : containingFile.getName();
            }

            @NotNull
            @Override
            public Icon getIcon(boolean unused) {
                if (isDeclaration()) {
                    if (QPsiUtil.getFunctionDefinition(QVariableElementImpl.this).isPresent()) {
                        return KdbIcons.Node.functionPublic;
                    } else {
                        return KdbIcons.Node.variablePublic;
                    }
                }
                return KdbIcons.Main.File;
            }
        };
    }
}
