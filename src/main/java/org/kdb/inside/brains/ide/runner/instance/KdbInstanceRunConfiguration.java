package org.kdb.inside.brains.ide.runner.instance;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RefactoringListenerProvider;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.ScriptFileUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import org.apache.commons.io.FilenameUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.runner.KdbRunConfigurationBase;
import org.kdb.inside.brains.psi.QFile;

public class KdbInstanceRunConfiguration extends KdbRunConfigurationBase implements RefactoringListenerProvider {
    private static final String SCRIPT_ARGUMENTS = "script_arguments";
    private String scriptArguments;

    protected KdbInstanceRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super("Kdb Script Run Configuration", project, factory);
    }

    @Nullable
    private static String getPathByElement(@NotNull PsiElement element) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }
        final VirtualFile f = file.getVirtualFile();
        if (f == null) {
            return null;
        }
        return ScriptFileUtil.getScriptFilePath(f);
    }

    public String getScriptArguments() {
        return scriptArguments;
    }

    public void setScriptArguments(String scriptArguments) {
        this.scriptArguments = scriptArguments;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new KdbInstanceRunningState(this, getExecutionModule(), environment);
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        addNonEmptyElement(element, SCRIPT_ARGUMENTS, scriptArguments);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        scriptArguments = JDOMExternalizerUtil.readCustomField(element, SCRIPT_ARGUMENTS);
    }

    @Override
    public @Nullable @NlsActions.ActionText String suggestedName() {
        return FilenameUtils.getName(getScriptName());
    }

    @Nullable
    @Override
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        if (!(element instanceof QFile)) {
            return null;
        }

        final String pathByElement = getPathByElement(element);
        if (pathByElement == null) {
            return null;
        }

        final String scriptName = getScriptName();
        final String independentName = FileUtil.toSystemIndependentName(scriptName);
        final String independentPathByElement = FileUtil.toSystemIndependentName(pathByElement);
        if (!independentName.equals(independentPathByElement)) {
            return null;
        }

        return new RefactoringElementAdapter() {
            @Override
            protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                if (newElement instanceof QFile file) {
                    final String scriptFilePath = ScriptFileUtil.getScriptFilePath(file.getVirtualFile());
                    setScriptName(FileUtil.toSystemDependentName(scriptFilePath));
                }
            }

            @Override
            public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                elementRenamedOrMoved(newElement);
            }
        };
    }

    @NotNull
    @Override
    public KdbInstanceRunSettingsEditor getConfigurationEditor() {
        return new KdbInstanceRunSettingsEditor(getProject());
    }
}