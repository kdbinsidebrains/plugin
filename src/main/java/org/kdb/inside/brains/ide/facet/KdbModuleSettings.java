package org.kdb.inside.brains.ide.facet;

import org.kdb.inside.brains.QVersion;

public class KdbModuleSettings {
    private QVersion languageVersion = QVersion.DEFAULT;

    public KdbModuleSettings() {
    }

    public QVersion getLanguageVersion() {
        return languageVersion;
    }

    public void setLanguageVersion(QVersion languageVersion) {
        this.languageVersion = languageVersion;
    }
}
