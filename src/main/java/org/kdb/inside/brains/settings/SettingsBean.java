package org.kdb.inside.brains.settings;

public interface SettingsBean<T extends SettingsBean<T>> {
    void copyFrom(T t);


}
