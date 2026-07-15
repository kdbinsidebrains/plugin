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
    @Attribute
    private Boolean tls;
    @Attribute
    private Boolean zip;
    @Attribute
    private Boolean async;
    @Attribute
    private Integer timeout;
    @Attribute
    private Boolean heartbeat;
    @Attribute
    private Integer heartbeatInterval;
    @Attribute
    private Integer heartbeatTimeout;
    @Attribute
    private Boolean autoReconnect;

    public static final int DEFAULT_TIMEOUT = 1000;
    public static final boolean DEFAULT_TLS = false;
    public static final boolean DEFAULT_ZIP = false;
    public static final boolean DEFAULT_ASYNC = false;
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final boolean DEFAULT_HEARTBEAT = true;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_SEC = 30;
    public static final int DEFAULT_HEARTBEAT_TIMEOUT_MS = 5000;
    public static final boolean DEFAULT_AUTO_RECONNECT = false;

    public static final int MIN_HEARTBEAT_INTERVAL_SEC = 5;
    public static final int MIN_HEARTBEAT_TIMEOUT_MS = 500;

    public static final InstanceOptions INHERITED = new InstanceOptions();

    @Attribute
    private String encoding;

    public InstanceOptions() {
    }

    private InstanceOptions(Boolean tls, Boolean zip, Boolean async, Integer timeout, String encoding, Boolean heartbeat, Integer heartbeatInterval, Integer heartbeatTimeout, Boolean autoReconnect) {
        this.tls = tls;
        this.zip = zip;
        this.async = async;
        this.timeout = timeout;
        this.encoding = encoding;
        this.heartbeat = heartbeat;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatTimeout = heartbeatTimeout;
        this.autoReconnect = autoReconnect;
    }

    public static InstanceOptions defaultOptions() {
        return new InstanceOptions(DEFAULT_TLS, DEFAULT_ZIP, DEFAULT_ASYNC, DEFAULT_TIMEOUT, DEFAULT_ENCODING, DEFAULT_HEARTBEAT, DEFAULT_HEARTBEAT_INTERVAL_SEC, DEFAULT_HEARTBEAT_TIMEOUT_MS, DEFAULT_AUTO_RECONNECT);
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

            final String encoding = el.getAttributeValue("encoding");
            if (encoding != null) {
                b.encoding(encoding);
            }

            final String heartbeat = el.getAttributeValue("heartbeat");
            if (heartbeat != null) {
                b.heartbeat(Boolean.valueOf(heartbeat));
            }

            final String heartbeatInterval = el.getAttributeValue("heartbeatInterval");
            if (heartbeatInterval != null) {
                b.heartbeatInterval(Integer.decode(heartbeatInterval.trim()));
            }

            final String heartbeatTimeout = el.getAttributeValue("heartbeatTimeout");
            if (heartbeatTimeout != null) {
                b.heartbeatTimeout(Integer.decode(heartbeatTimeout.trim()));
            }

            final String autoReconnect = el.getAttributeValue("autoReconnect");
            if (autoReconnect != null) {
                b.autoReconnect(Boolean.valueOf(autoReconnect));
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
                .encoding(resolve(options, o -> o.encoding))
                .heartbeat(resolve(options, o -> o.heartbeat))
                .heartbeatInterval(resolve(options, o -> o.heartbeatInterval))
                .heartbeatTimeout(resolve(options, o -> o.heartbeatTimeout))
                .autoReconnect(resolve(options, o -> o.autoReconnect))
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

    @Transient
    public boolean hasEncoding() {
        return encoding != null;
    }

    @Transient
    public String getSafeEncoding() {
        return hasEncoding() ? encoding : DEFAULT_ENCODING;
    }

    @Transient
    public boolean hasHeartbeat() {
        return heartbeat != null;
    }

    @Transient
    public boolean isSafeHeartbeatEnabled() {
        return hasHeartbeat() ? heartbeat : DEFAULT_HEARTBEAT;
    }

    @Transient
    public boolean hasHeartbeatInterval() {
        return heartbeatInterval != null;
    }

    @Transient
    public int getSafeHeartbeatIntervalSec() {
        return hasHeartbeatInterval() ? heartbeatInterval : DEFAULT_HEARTBEAT_INTERVAL_SEC;
    }

    @Transient
    public boolean hasHeartbeatTimeout() {
        return heartbeatTimeout != null;
    }

    @Transient
    public int getSafeHeartbeatTimeoutMs() {
        return hasHeartbeatTimeout() ? heartbeatTimeout : DEFAULT_HEARTBEAT_TIMEOUT_MS;
    }

    @Transient
    public boolean hasAutoReconnect() {
        return autoReconnect != null;
    }

    @Transient
    public boolean isSafeAutoReconnect() {
        return hasAutoReconnect() ? autoReconnect : DEFAULT_AUTO_RECONNECT;
    }

    @Override
    public void copyFrom(InstanceOptions options) {
        this.tls = options.tls;
        this.zip = options.zip;
        this.async = options.async;
        this.timeout = options.timeout;
        this.encoding = options.encoding;
        this.heartbeat = options.heartbeat;
        this.heartbeatInterval = options.heartbeatInterval;
        this.heartbeatTimeout = options.heartbeatTimeout;
        this.autoReconnect = options.autoReconnect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstanceOptions that)) return false;
        return Objects.equals(tls, that.tls) && Objects.equals(zip, that.zip) && Objects.equals(async, that.async) && Objects.equals(timeout, that.timeout) && Objects.equals(encoding, that.encoding)
                && Objects.equals(heartbeat, that.heartbeat) && Objects.equals(heartbeatInterval, that.heartbeatInterval) && Objects.equals(heartbeatTimeout, that.heartbeatTimeout) && Objects.equals(autoReconnect, that.autoReconnect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tls, zip, async, timeout, encoding, heartbeat, heartbeatInterval, heartbeatTimeout, autoReconnect);
    }

    @Override
    public String toString() {
        return "InstanceOptions{" +
                "tls=" + tls +
                ", zip=" + zip +
                ", async=" + async +
                ", timeout=" + timeout +
                ", encoding=" + encoding +
                ", heartbeat=" + heartbeat +
                ", heartbeatInterval=" + heartbeatInterval +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", autoReconnect=" + autoReconnect +
                '}';
    }

    public String toParameters() {
        final List<String> b = new ArrayList<>(9);
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
        if (hasEncoding()) {
            b.add("encoding=" + encoding);
        }
        if (hasHeartbeat()) {
            b.add("heartbeat=" + heartbeat);
        }
        if (hasHeartbeatInterval()) {
            b.add("heartbeatInterval=" + heartbeatInterval);
        }
        if (hasHeartbeatTimeout()) {
            b.add("heartbeatTimeout=" + heartbeatTimeout);
        }
        if (hasAutoReconnect()) {
            b.add("autoReconnect=" + autoReconnect);
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
        if (encoding != null) {
            el.setAttribute("encoding", encoding);
        }
        if (heartbeat != null) {
            el.setAttribute("heartbeat", String.valueOf(heartbeat));
        }
        if (heartbeatInterval != null) {
            el.setAttribute("heartbeatInterval", String.valueOf(heartbeatInterval));
        }
        if (heartbeatTimeout != null) {
            el.setAttribute("heartbeatTimeout", String.valueOf(heartbeatTimeout));
        }
        if (autoReconnect != null) {
            el.setAttribute("autoReconnect", String.valueOf(autoReconnect));
        }
    }

    public static class Builder {
        private Boolean tls;
        private Boolean zip;
        private Boolean async;
        private Integer timeout;
        private String encoding;
        private Boolean heartbeat;
        private Integer heartbeatInterval;
        private Integer heartbeatTimeout;
        private Boolean autoReconnect;

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

        public Builder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder heartbeat(Boolean heartbeat) {
            this.heartbeat = heartbeat;
            return this;
        }

        public Builder heartbeatInterval(Integer heartbeatInterval) {
            if (heartbeatInterval != null && heartbeatInterval < MIN_HEARTBEAT_INTERVAL_SEC) {
                throw new IllegalArgumentException("Heartbeat interval can't be less than " + MIN_HEARTBEAT_INTERVAL_SEC);
            }
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public Builder heartbeatTimeout(Integer heartbeatTimeout) {
            if (heartbeatTimeout != null && heartbeatTimeout < MIN_HEARTBEAT_TIMEOUT_MS) {
                throw new IllegalArgumentException("Heartbeat timeout can't be less than " + MIN_HEARTBEAT_TIMEOUT_MS);
            }
            this.heartbeatTimeout = heartbeatTimeout;
            return this;
        }

        public Builder autoReconnect(Boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
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
            if (encoding == null) {
                encoding = DEFAULT_ENCODING;
            }
            if (heartbeat == null) {
                heartbeat = DEFAULT_HEARTBEAT;
            }
            if (heartbeatInterval == null) {
                heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL_SEC;
            }
            if (heartbeatTimeout == null) {
                heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT_MS;
            }
            if (autoReconnect == null) {
                autoReconnect = DEFAULT_AUTO_RECONNECT;
            }
            return this;
        }

        public InstanceOptions create() {
            return new InstanceOptions(tls, zip, async, timeout, encoding, heartbeat, heartbeatInterval, heartbeatTimeout, autoReconnect);
        }
    }
}