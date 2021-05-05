package org.kdb.inside.brains.ide.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ui.configuration.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class KdbModuleEditorsProvider implements ModuleConfigurationEditorProvider {
    @Override
    public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state) {
        final var rootModel = state.getRootModel();
        final Module module = rootModel.getModule();
        if (!(ModuleType.get(module) instanceof KdbModuleType)) {
            return ModuleConfigurationEditor.EMPTY;
        }

        final ProjectSdksModel sdksModel = new ProjectSdksModel();
        final SdkComboBoxModel sdkComboBoxModel = SdkComboBoxModel.createSdkComboBoxModel(module.getProject(), sdksModel, sdkTypeId -> sdkTypeId instanceof KdbSdkType);

        final SdkComboBox sdkComboBox = new SdkComboBox(sdkComboBoxModel);
        sdkComboBox.showNoneSdkItem();
        sdkComboBox.showProjectSdkItem();

        final var commonContentEntriesEditor = new CommonContentEntriesEditor(module.getName(), state, JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE) {
            @Override
            protected void addAdditionalSettingsToPanel(final JPanel mainPanel) {
                final var formBuilder = FormBuilder.createFormBuilder();
                formBuilder.addLabeledComponent("Module SDK: ", sdkComboBox);

                final var panel = formBuilder.getPanel();
                panel.setBorder(JBUI.Borders.empty(0, 10, 5, 10));

                mainPanel.add(panel, BorderLayout.NORTH);
            }

            @Override
            public boolean isModified() {
                return super.isModified() || !Objects.equals(rootModel.getSdk(), sdkComboBox.getSelectedSdk());
            }

            @Override
            public void apply() {
                ModuleRootModificationUtil.updateModel(module, root -> {
                    final Sdk selectedSdk = sdkComboBox.getSelectedSdk();

                    final SdkListItem selectedItem = sdkComboBox.getSelectedItem();
                    if (selectedItem == sdkComboBox.showProjectSdkItem()) {
                        root.inheritSdk();
                    } else {
                        root.setSdk(selectedSdk);
                    }
                });
            }

            @Override
            public void reset() {
                sdksModel.reset(module.getProject());

                if (rootModel.isSdkInherited()) {
                    sdkComboBox.setSelectedItem(sdkComboBox.showProjectSdkItem());
                } else {
                    final String sdkName = rootModel.getSdkName();
                    if (sdkName == null) {
                        sdkComboBox.setSelectedItem(sdkComboBox.showNoneSdkItem());
                    } else {
                        sdkComboBox.setSelectedSdk(sdkName);
                    }
                }
            }
        };

        return new ModuleConfigurationEditor[]{
                commonContentEntriesEditor
        };
    }
}
