package org.kdb.inside.brains.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Objects;

public class KdbInstance extends InstanceItem implements CredentialsItem {
    private String host;
    private int port;
    private String credentials;
    private InstanceOptions options;

    public KdbInstance(@NotNull String name, @NotNull String host, int port, @Nullable String credentials) {
        this(name, host, port, credentials, InstanceOptions.INHERITED);
    }

    public KdbInstance(@NotNull String name, @NotNull String host, int port, @Nullable String credentials, @NotNull InstanceOptions options) {
        super(name);
        this.host = Objects.requireNonNull(host);
        this.port = validatePort(port);
        this.credentials = credentials;
        this.options = options;
    }

    public int getPort() {
        return port;
    }

    private static int validatePort(int port) {
        if (port < 0 || port >= 65536) {
            throw new IllegalArgumentException("Port is out of range");
        }
        return port;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = validatePort(port);
        notifyItemUpdated();
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    public InstanceOptions getOptions() {
        return options;
    }

    /**
     * Returns only `:host:port without username and password.
     */
    public String toAddress() {
        return "`:" + host + ":" + port;
    }

    /**
     * Returns only `:host:port:username:***
     */
    public String toSymbol() {
        final String s = toAddress();
        if (credentials == null) {
            return s;
        } else {
            final int i = credentials.indexOf(':');
            if (i < 0) {
                return s;
            }
            return s + ":" + credentials.substring(0, i) + ":***";
        }
    }

    public String toTransferableSymbol() {
        return "`:" + host + ":" + port + (credentials != null ? ":" + credentials : "");
    }

    public String toQualifiedSymbol() {
        final String s = toTransferableSymbol();
        if (options == null) {
            return s;
        }
        final String parameters = options.toParameters();
        if (parameters.isEmpty()) {
            return s;
        }
        return s + "?" + parameters;
    }

    public void setHost(String host) {
        this.host = Objects.requireNonNull(host);
        notifyItemUpdated();
    }

    public void updateFrom(KdbInstance instance) {
        setName(instance.getName());
        this.host = instance.host;
        this.port = instance.port;
        this.credentials = instance.credentials;
        this.options = instance.options;

        notifyItemUpdated();
    }

    public void updateAddress(String value) {
        if (value.charAt(0) == '`') {
            value = value.substring(1);
        }
        if (value.charAt(0) == ':') {
            value = value.substring(1);
        }

        int i = value.indexOf(':');
        if (i < 0) {
            throw new IllegalArgumentException("No port");
        }
        // Order is important or host can be changed without the port
        port = Integer.parseInt(value.substring(i + 1));
        host = value.substring(0, i);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        final DataFlavor[] transferDataFlavors = super.getTransferDataFlavors();
        final DataFlavor[] res = new DataFlavor[transferDataFlavors.length + 1];
        System.arraycopy(transferDataFlavors, 0, res, 0, transferDataFlavors.length);
        res[transferDataFlavors.length] = DataFlavor.stringFlavor;
        return res;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.stringFlavor) || super.isDataFlavorSupported(flavor);
    }

    public static KdbInstance parseInstance(String txt) {
        String url = txt.strip();
        if (url.isBlank()) {
            return null;
        }

        if (url.charAt(0) == '`') {
            url = url.substring(1);
        }
        if (url.charAt(0) == ':') {
            url = url.substring(1);
        }

        final String[] split = url.split(":");
        if (split.length < 2) {
            return null;
        }

        int port;
        try {
            port = Integer.parseInt(split[1]);
        } catch (NumberFormatException ex) {
            return null;
        }

        String credentials = null;
        if (split.length > 2) {
            String[] a = new String[split.length - 2];
            System.arraycopy(split, 2, a, 0, split.length - 2);
            credentials = String.join(":", a);
        }

        // TODO: parse options here
        try {
            return new KdbInstance(txt, split[0], port, credentials);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return toTransferableSymbol();
        }
        return super.getTransferData(flavor);
    }

    @Override
    public @NotNull KdbInstance copy() {
        return new KdbInstance(getName(), host, port, credentials, options);
    }

    @Override
    public String toString() {
        return getName() + "@" + toSymbol();
    }
}
