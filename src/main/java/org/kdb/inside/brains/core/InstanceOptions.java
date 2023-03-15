package org.kdb.inside.brains.core;

import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class InstanceOptions implements SettingsBean<InstanceOptions> {
    private int timeout = 1000;
    private boolean tls = false;
    private boolean compression = false;
    private boolean asynchronous = false;

    public InstanceOptions() {
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
        return "tls=" + tls + "&timeout=" + timeout + "&compression=" + compression + "&asynchronous" + asynchronous;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public InstanceOptions copy() {
        final InstanceOptions o = new InstanceOptions();
        o.copyFrom(this);
        return o;
    }

    @Override
    public void copyFrom(InstanceOptions options) {
        tls = options.tls;
        timeout = options.timeout;
        compression = options.compression;
        asynchronous = options.asynchronous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstanceOptions)) return false;
        InstanceOptions that = (InstanceOptions) o;
        return timeout == that.timeout && tls == that.tls && compression == that.compression && asynchronous == that.asynchronous;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeout, tls, compression, asynchronous);
    }

    @Override
    public String toString() {
        return "InstanceOptions{" + "timeout=" + timeout + ", tls=" + tls + ", compression=" + compression + ", asynchronous=" + asynchronous + '}';
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
