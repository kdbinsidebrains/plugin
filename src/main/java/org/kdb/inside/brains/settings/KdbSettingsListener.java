package org.kdb.inside.brains.settings;

public interface KdbSettingsListener {
    void settingsChanged(KdbSettingsService service, SettingsBean<?> bean);
}
