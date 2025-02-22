package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("RawUseOfParameterized")
public class QSpecTestLocator implements SMTestLocator {
    private static final String PROTOCOL_SCRIPT = "qspec:script";
    private static final String PROTOCOL_SUITE = "qspec:suite";
    private static final String PROTOCOL_TEST = "qspec:test";
    private static final List<@NotNull Location> NO_LOCATION = List.of();

    public static final QSpecTestLocator INSTANCE = new QSpecTestLocator();

    public static final String QSPEC_EXPECTATION_FUNCTION = "should";
    public static final String QSPEC_SPECIFICATION_FUNCTION = ".tst.desc";

    private QSpecTestLocator() {
    }

    @Override
    public @NotNull List<Location> getLocation(@NotNull String protocol, @NotNull String path, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        if (PROTOCOL_SCRIPT.equals(protocol)) {
            return getLocation(path, project, this::findPsiFile);
        } else if (PROTOCOL_SUITE.equals(protocol)) {
            return getLocation(path, project, this::findSpecification);
        } else if (PROTOCOL_TEST.equals(protocol)) {
            return getLocation(path, project, this::findExpectation);
        }
        return NO_LOCATION;
    }

    private @NotNull List<Location> getLocation(@NotNull String path, @NotNull Project project, BiFunction<String, Project, PsiElement> finder) {
        final PsiElement element = finder.apply(path, project);
        if (element == null) {
            return NO_LOCATION;
        }
        if (element instanceof QVarReference ref) {
            return List.of(PsiLocation.fromPsiElement(ref.getFirstChild()));
        }
        return List.of(PsiLocation.fromPsiElement(element));
    }

    private @Nullable QVarReference findExpectation(@NotNull String path, @NotNull Project project) {
        final int i = path.indexOf("]/[");
        if (i < 0) {
            return null;
        }

        final QVarReference specElement = findSpecification(path.substring(0, i + 1), project);
        if (specElement == null) {
            return null;
        }

        if (!(specElement.getParent().getParent() instanceof QInvokeFunction invoke)) {
            return null;
        }

        if (!(invoke.getExpression() instanceof QLambdaExpr lambda)) {
            return null;
        }

        final QExpressions expressions = lambda.getExpressions();
        if (expressions == null) {
            return null;
        }

        final String name = '"' + path.substring(i + 3, path.length() - 1) + '"';

        final List<QExpression> expressionList = expressions.getExpressionList();
        for (QExpression exp : expressionList) {
            if (!(exp instanceof QInvokeFunction f)) {
                continue;
            }
            final QVarReference ref = getInvokeReference(f, QSPEC_EXPECTATION_FUNCTION, name);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    private @Nullable QVarReference findSpecification(@NotNull String path, @NotNull Project project) {
        final int i = path.indexOf('?');
        if (i < 0) {
            return null;
        }

        final PsiFileSystemItem fileItem = findPsiFile(path.substring(0, i), project);
        if (!(fileItem instanceof QFile file)) {
            return null;
        }

        final QInvokeFunction[] invokes = PsiTreeUtil.getChildrenOfType(file, QInvokeFunction.class);
        if (invokes == null) {
            return null;
        }

        final String suiteName = '"' + path.substring(i + 2, path.length() - 1) + '"';
        for (QInvokeFunction invoke : invokes) {
            final QVarReference ref = getInvokeReference(invoke, QSPEC_SPECIFICATION_FUNCTION, suiteName);
            if (ref != null) {
                return ref;
            }
        }
        return null;
    }

    private @Nullable PsiFileSystemItem findPsiFile(@NotNull String path, @NotNull Project project) {
        final VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path));
        if (file == null) {
            return null;
        }
        final PsiManager instance = PsiManager.getInstance(project);
        return file.isDirectory() ? instance.findDirectory(file) : instance.findFile(file);
    }

    private static @Nullable QVarReference getInvokeReference(QInvokeFunction invoke, String name, String value) {
        final QCustomFunction cf = invoke.getCustomFunction();
        if (cf == null) {
            return null;
        }

        final QExpression expression = cf.getExpression();
        if (!(expression instanceof QVarReference ref)) {
            return null;
        }

        if (!name.equals(ref.getQualifiedName())) {
            return null;
        }

        final List<QArguments> arguments = invoke.getArgumentsList();
        if (arguments.size() != 1) {
            return null;
        }

        final List<QExpression> expressions = arguments.get(0).getExpressions();
        if (expressions.size() != 1) {
            return null;
        }

        final QExpression qExpression = expressions.get(0);
        if (!(qExpression instanceof QLiteralExpr lit)) {
            return null;
        }

        if (value.equals(lit.getText())) {
            return ref;
        }
        return null;
    }
}
