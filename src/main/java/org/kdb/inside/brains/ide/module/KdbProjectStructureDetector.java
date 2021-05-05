package org.kdb.inside.brains.ide.module;

import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.util.projectWizard.importSources.DetectedContentRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.sdk.KdbSdkPanel;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.List;

public class KdbProjectStructureDetector extends ProjectStructureDetector {
    @Override
    public String getDetectorId() {
        return "KDB+ Q";
    }

    @NotNull
    @Override
    public DirectoryProcessingResult detectRoots(@NotNull File dir, File @NotNull [] children, @NotNull File base, @NotNull List<DetectedProjectRoot> result) {
        // ignore all hidden
        if (dir.getName().startsWith(".")) {
            return DirectoryProcessingResult.SKIP_CHILDREN;
        }

        for (File child : children) {
            if (QFileType.hasExtension(child)) {
                result.add(new DetectedContentRoot(dir, "KDB+ Q Content", KdbModuleType.getModuleType()));
                return DirectoryProcessingResult.SKIP_CHILDREN;
            }
        }
        return DirectoryProcessingResult.PROCESS_CHILDREN;
    }

    @Override
    public void setupProjectStructure(@NotNull Collection<DetectedProjectRoot> roots, @NotNull ProjectDescriptor projectDescriptor, @NotNull ProjectFromSourcesBuilder builder) {
        if (!builder.hasRootsFromOtherDetectors(this)) {
            builder.setupModulesByContentRoots(projectDescriptor, roots);
        }
    }

    @Override
    public List<ModuleWizardStep> createWizardSteps(ProjectFromSourcesBuilder builder, ProjectDescriptor projectDescriptor, Icon stepIcon) {
        final WizardContext context = builder.getContext();
        if (context.isCreatingNewProject()) {
            final KdbSdkPanel kdbSdkPanel = new KdbSdkPanel(true);

            final ModuleWizardStep step = new ModuleWizardStep() {
                @Override
                public String getName() {
                    return "Project KDB SDK";
                }

                @Override
                public JComponent getComponent() {
                    return kdbSdkPanel;
                }

                @Override
                public JComponent getPreferredFocusedComponent() {
                    return kdbSdkPanel.getPreferredFocusedComponent();
                }

                @Override
                public Icon getIcon() {
                    return context.getStepIcon();
                }

                @Override
                public void updateDataModel() {
                    builder.getContext().setProjectJdk(kdbSdkPanel.getSdk());
                }
            };
            return List.of(step);
        }
        return List.of();
    }
}
