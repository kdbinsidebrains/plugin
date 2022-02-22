package org.kdb.inside.brains.lang.docs;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.QWord;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QTypes;
import org.kdb.inside.brains.psi.QVariable;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static com.intellij.lang.documentation.DocumentationMarkup.*;

public final class QDocumentationProvider extends AbstractDocumentationProvider {
    private static final Set<IElementType> ALLOWED_ELEMENTS = Set.of(
            QTypes.QUERY_TYPE,
            QTypes.QUERY_BY,
            QTypes.QUERY_FROM,
            QTypes.UNARY_FUNCTION,
            QTypes.BINARY_FUNCTION,
            QTypes.COMPLEX_FUNCTION,
            QTypes.COMMAND_SYSTEM,
            QTypes.COMMAND_IMPORT,
            QTypes.COMMAND_CONTEXT,
            QTypes.VAR_REFERENCE,
            QTypes.VAR_DECLARATION
    );

    private static final String SYSTEM_DOC_STYLE = "<style>.language-txt, .language-q { color: #657b83; }</style>";

    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        final QWord word = getWord(element);
        if (word != null) {
            return createSystemDoc(word);
        }

        if (element instanceof QVariable) {
            return createVariableDoc((QVariable) element);
        }

        return null;
    }

    private String createVariableDoc(QVariable variable) {
        final PsiReference[] references = variable.getReferences();
        if (references.length != 1) {
            return null;
        }

        final PsiElement resolve = references[0].resolve();
        if (resolve == null) {
            return null;
        }

        final QAssignmentExpr context = PsiTreeUtil.getContextOfType(resolve, QAssignmentExpr.class);
        if (context == null) {
            return null;
        }

        final QVariableDoc doc = QVariableDoc.from(context);
        if (doc == null) {
            return context.getText();
        }
        return doc.toHtml();
    }

    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        final QWord word = getWord(element);
        if (word != null) {
            if (word.getDocsUrl() != null) {
                return List.of("https://code.kx.com/q/" + word.getDocsUrl());
            }
        }
        return null;
    }

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement == null) {
            return null;
        }

        if (contextElement instanceof PsiComment) {
            return getCustomDocumentationElement(editor, file, getCommentedVariable(contextElement), targetOffset);
        }

        final IElementType elementType = contextElement.getNode().getElementType();
        if (ALLOWED_ELEMENTS.contains(elementType)) {
            return contextElement;
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
    }

    private PsiElement getCommentedVariable(PsiElement element) {
        final PsiElement e = PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
        if (e instanceof QAssignmentExpr) {
            return ((QAssignmentExpr) e).getVarDeclaration();
        }
        return null;
    }

    private QWord getWord(PsiElement element) {
        return QLanguage.getWord(element.getText());
    }

    private String getFilename(QWord word) {
        final String name = word.getName();

        final StringBuilder b = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            if (c == '\\' || c == '.') {
                b.append('_');
            } else if (Character.isUpperCase(c)) {
                b.append('_').append(Character.toLowerCase(c));
            } else {
                b.append(c);
            }
        }
        final String s = b.toString();
        return s + ".html";
    }

    private String createSystemDoc(QWord s) {
        return SYSTEM_DOC_STYLE + DEFINITION_START + "<b>" + s.getName() + ": </b>" + s.getDescription() + DEFINITION_END + CONTENT_START + loadWordDescription(s) + CONTENT_END;
    }

    private String loadWordDescription(QWord word) {
        try {
            final URL resource = getClass().getResource("/org/kdb/inside/brains/words/docs/" + word.getType().getCode() + "/" + getFilename(word));
            if (resource == null) {
                return "No internal documentation found. Try to check <a href=\"https://code.kx.com\">code.kx.com</a> website.";
            }

            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }
}
