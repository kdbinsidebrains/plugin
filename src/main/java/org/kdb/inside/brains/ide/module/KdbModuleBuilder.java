package org.kdb.inside.brains.ide.module;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import icons.KdbIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.module.facet.KdbFacetType;
import org.kdb.inside.brains.ide.sdk.KdbSdkPanel;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import javax.swing.*;

public class KdbModuleBuilder extends ModuleBuilder implements ModuleBuilderListener {
    public KdbModuleBuilder() {
        addListener(this);
    }

    @Override
    protected @Nls(capitalization = Nls.Capitalization.Title) String getModuleTypeName() {
        return super.getModuleTypeName();
    }

    @Override
    public String getBuilderId() {
        return "KDB_MODULE_BUILDER";
    }

    @Override
    public Icon getNodeIcon() {
        return KdbIcons.Main.Module;
    }

    @Override
    public ModuleType<?> getModuleType() {
        return KdbModuleType.getModuleType();
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdkType) {
        return sdkType instanceof KdbSdkType;
    }

    @Override
    public String getDescription() {
        return "Module for Q language sources. Add your root folders as content enties to be able to search and complete your code.";
    }

    @Override
    public String getGroupName() {
        return "kdb+q";
    }

    @Override
    public String getPresentableName() {
        return "KDB+ Q";
    }

    @Override
    public @Nullable ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new ModuleWizardStep() {
            final KdbSdkPanel kdbSdkPanel = new KdbSdkPanel(true);

            @Override
            public JComponent getComponent() {
                return kdbSdkPanel;
            }

            @Override
            public void updateDataModel() {
                context.setProjectJdk(kdbSdkPanel.getSdk());
            }
        };
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel rootModel) {
        doAddContentEntry(rootModel);
        rootModel.inheritSdk();
    }

    @Override
    public void moduleCreated(@NotNull Module module) {
        final var type = KdbFacetType.getInstance();
        final var typeId = type.getId();

        final FacetManager facetManager = FacetManager.getInstance(module);
        final Facet<?> facet = facetManager.getFacetByType(typeId);
        if (facet == null) {
            final var model = facetManager.createModifiableModel();

            final var kdbFacet = facetManager.addFacet(type, type.getDefaultFacetName(), null);
            model.addFacet(kdbFacet);
            model.commit();
        }
    }
}
