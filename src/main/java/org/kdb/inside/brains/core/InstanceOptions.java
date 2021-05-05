package org.kdb.inside.brains.core;

import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class InstanceOptions implements SettingsBean<InstanceOptions> {
    private int timeout = 1000;
    private boolean tls = false;
    private boolean compression = false;

    public InstanceOptions() {
    }

    public InstanceOptions(int timeout, boolean tls, boolean compression) {
        this.tls = tls;
        this.timeout = timeout;
        this.compression = compression;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public String toParameters() {
        return "tls=" + tls + "&timeout=" + timeout + "&compression=" + compression;
    }

    public InstanceOptions copy() {
        return new InstanceOptions(timeout, tls, compression);
    }

    @Override
    public void copyFrom(InstanceOptions options) {
        tls = options.tls;
        timeout = options.timeout;
        compression = options.compression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceOptions options = (InstanceOptions) o;
        return timeout == options.timeout && tls == options.tls && compression == options.compression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeout, tls, compression);
    }

    @Override
    public String toString() {
        return "InstanceOptions{" +
                "timeout=" + timeout +
                ", tls=" + tls +
                ", compression=" + compression +
                '}';
    }

    public static InstanceOptions resolveOptions(KdbInstance instance) {
        InstanceOptions options = instance.getOptions();
        if (options != null) {
            return options;
        }
        final KdbScope scope = instance.getScope();
        if (scope != null && scope.getOptions() != null) {
            return scope.getOptions();
        }
        return KdbSettingsService.getInstance().getInstanceOptions();
    }
}
