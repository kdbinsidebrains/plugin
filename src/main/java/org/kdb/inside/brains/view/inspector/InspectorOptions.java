package org.kdb.inside.brains.view.inspector;

import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class InspectorOptions implements SettingsBean<InspectorOptions> {
    private boolean scanOnConnect = true;

    public boolean isScanOnConnect() {
        return scanOnConnect;
    }

    public void setScanOnConnect(boolean scanOnConnect) {
        this.scanOnConnect = scanOnConnect;
    }

    @Override
    public void copyFrom(InspectorOptions options) {
        this.scanOnConnect = options.scanOnConnect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InspectorOptions that = (InspectorOptions) o;
        return scanOnConnect == that.scanOnConnect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanOnConnect);
    }

    @Override
    public String toString() {
        return "InspectorOptions{" +
                "scanOnConnect=" + scanOnConnect +
                '}';
    }
}
