package org.kdb.inside.brains.ide.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KdbFacetConfiguration implements FacetConfiguration, PersistentStateComponent<KdbModuleSettings> {
    private final KdbModuleSettings mySettings = new KdbModuleSettings();

    @Override
    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[]{new KdbFacetEditorTab(mySettings)};
    }

    @Override
    public @Nullable KdbModuleSettings getState() {
        return mySettings;
    }

    @Override
    public void loadState(@NotNull KdbModuleSettings state) {
        XmlSerializerUtil.copyBean(state, mySettings);
    }
}
