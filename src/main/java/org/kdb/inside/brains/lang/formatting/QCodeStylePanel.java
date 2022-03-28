package org.kdb.inside.brains.lang.formatting;

import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.kdb.inside.brains.QLanguage;

public class QCodeStylePanel extends TabbedLanguageCodeStylePanel {
    public QCodeStylePanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
        super(QLanguage.INSTANCE, currentSettings, settings);
    }

    @Override
    protected void initTabs(CodeStyleSettings settings) {
        addIndentOptionsTab(settings);
        addSpacesTab(settings);
//        addBlankLinesTab(settings);
        addWrappingAndBracesTab(settings);
    }
}