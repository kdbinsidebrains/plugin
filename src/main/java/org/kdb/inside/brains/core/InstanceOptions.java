package org.kdb.inside.brains.core;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class InstanceOptions implements SettingsBean<InstanceOptions> {
    public static final int DEFAULT_TIMEOUT = 1000;
    public static final boolean DEFAULT_TLS = false;
    public static final boolean DEFAULT_ZIP = false;
    public static final boolean DEFAULT_ASYNC = false;
    public static final InstanceOptions INHERITED = new InstanceOptions();
    @Attribute
    private Boolean tls;
    @Attribute
    private Boolean zip;
    @Attribute
    private Boolean async;
    @Attribute
    private Integer timeout;

    public InstanceOptions() {
    }

    private InstanceOptions(Boolean tls, Boolean zip, Boolean async, Integer timeout) {
        this.tls = tls;
        this.zip = zip;
        this.async = async;
        this.timeout = timeout;
    }

    public static InstanceOptions fromParameters(String params) {
        if (params == null || params.isBlank()) {
            return INHERITED;
        }

        final Element e = new Element("mock");
        for (String pair : params.split("&")) {
            final int idx = pair.indexOf("=");
            if (idx < 0) {
                continue;
            }
            final String name = pair.substring(0, idx);
            final String value = pair.substring(idx + 1);
            e.setAttribute(name, value);
        }
        return restore(e);
    }

    private static <T> T resolve(List<InstanceOptions> options, Function<InstanceOptions, T> extractor) {
        for (InstanceOptions option : options) {
            final T apply = extractor.apply(option);
            if (apply != null) {
                return apply;
            }
        }
        return null;
    }

    public static InstanceOptions restore(Element el) {
        if (el == null) {
            return INHERITED;
        }

        try {
            final Builder b = new Builder();
            final String timeout = el.getAttributeValue("timeout");
            if (timeout != null) {
                b.timeout(Integer.decode(timeout.trim()));
            }

            final String async = el.getAttributeValue("async");
            if (async != null) {
                b.async(Boolean.valueOf(async));
            }

            final String tls = el.getAttributeValue("tls");
            if (tls != null) {
                b.tls(Boolean.valueOf(tls));
            }

            String zip = el.getAttributeValue("zip");
            if (zip == null) {
                zip = el.getAttributeValue("compression");
            }
            if (zip != null) {
                b.zip(Boolean.valueOf(zip));
            }
            return b.create();
        } catch (Exception ignore) {
            return INHERITED;
        }
    }

    public static InstanceOptions resolveOptions(KdbScope scope) {
        return getInstanceOptions(collectOptions(scope));
    }

    public static InstanceOptions resolveOptions(KdbInstance instance) {
        return getInstanceOptions(collectOptions(instance));
    }

    private static InstanceOptions getInstanceOptions(List<InstanceOptions> options) {
        return new Builder()
                .tls(resolve(options, o -> o.tls))
                .zip(resolve(options, o -> o.zip))
                .async(resolve(options, o -> o.async))
                .timeout(resolve(options, o -> o.timeout))
                .safe()
                .create();
    }

    private static List<InstanceOptions> collectOptions(KdbInstance instance) {
        final List<InstanceOptions> options = new ArrayList<>();
        options.add(instance.getOptions());
        options.addAll(collectOptions(instance.getScope()));
        return options;
    }

    private static List<InstanceOptions> collectOptions(KdbScope scope) {
        final List<InstanceOptions> options = new ArrayList<>(2);
        if (scope != null) {
            options.add(scope.getOptions());
        }
        options.add(KdbSettingsService.getInstance().getInstanceOptions());
        return options;
    }

    // Safe properties
    @Transient
    public boolean hasTls() {
        return tls != null;
    }

    @Transient
    public boolean isSafeTls() {
        return hasTls() ? tls : DEFAULT_TLS;
    }

    @Transient
    public boolean hasZip() {
        return zip != null;
    }

    @Transient
    public boolean isSafeZip() {
        return hasZip() ? zip : DEFAULT_ZIP;
    }

    @Transient
    public boolean hasAsync() {
        return async != null;
    }

    @Transient
    public boolean isSafeAsync() {
        return hasAsync() ? async : DEFAULT_ASYNC;
    }

    @Transient
    public boolean hasTimeout() {
        return timeout != null;
    }

    @Transient
    public int getSafeTimeout() {
        return hasTimeout() ? timeout : DEFAULT_TIMEOUT;
    }

    @Override
    public void copyFrom(InstanceOptions options) {
        this.tls = options.tls;
        this.zip = options.zip;
        this.async = options.async;
        this.timeout = options.timeout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tls, zip, async, timeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstanceOptions)) return false;
        InstanceOptions that = (InstanceOptions) o;
        return Objects.equals(tls, that.tls) && Objects.equals(zip, that.zip) && Objects.equals(async, that.async) && Objects.equals(timeout, that.timeout);
    }

    @Override
    public String toString() {
        return "InstanceOptions{" +
                "tls=" + tls +
                ", zip=" + zip +
                ", async=" + async +
                ", timeout=" + timeout +
                '}';
    }

    public String toParameters() {
        final List<String> b = new ArrayList<>(4);
        if (hasTls()) {
            b.add("tls=" + tls);
        }
        if (hasZip()) {
            b.add("zip=" + zip);
        }
        if (hasAsync()) {
            b.add("async=" + async);
        }
        if (hasTimeout()) {
            b.add("timeout=" + timeout);
        }
        return String.join("&", b);
    }

    public void store(Element el) {
        if (tls != null) {
            el.setAttribute("tls", String.valueOf(tls));
        }
        if (zip != null) {
            el.setAttribute("zip", String.valueOf(zip));
        }
        if (async != null) {
            el.setAttribute("async", String.valueOf(async));
        }
        if (timeout != null) {
            el.setAttribute("timeout", String.valueOf(timeout));
        }
    }

    public static class Builder {
        private Boolean tls;
        private Boolean zip;
        private Boolean async;
        private Integer timeout;

        public Builder tls(Boolean tls) {
            this.tls = tls;
            return this;
        }

        public Builder zip(Boolean zip) {
            this.zip = zip;
            return this;
        }

        public Builder async(Boolean async) {
            this.async = async;
            return this;
        }

        public Builder timeout(Integer timeout) {
            if (timeout != null && timeout < DEFAULT_TIMEOUT) {
                throw new IllegalArgumentException("Timeout can't be less than " + DEFAULT_TIMEOUT);
            }
            this.timeout = timeout;
            return this;
        }

        public Builder safe() {
            if (tls == null) {
                tls = DEFAULT_TLS;
            }
            if (zip == null) {
                zip = DEFAULT_ZIP;
            }
            if (async == null) {
                async = DEFAULT_ASYNC;
            }
            if (timeout == null) {
                timeout = DEFAULT_TIMEOUT;
            }
            return this;
        }

        public InstanceOptions create() {
            return new InstanceOptions(tls, zip, async, timeout);
        }
    }
}