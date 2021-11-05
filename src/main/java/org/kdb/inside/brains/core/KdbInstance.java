package org.kdb.inside.brains.core;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Objects;

public class KdbInstance extends InstanceItem implements CredentialsItem {
    private String host;
    private int port;
    private String credentials;
    private InstanceOptions options;

    public KdbInstance(String name, String host, int port, String credentials, InstanceOptions options) {
        super(name);
        this.host = Objects.requireNonNull(host);
        if (port < 0 || port >= 65536) {
            throw new IllegalArgumentException("Port is out of range");
        }
        this.port = port;
        this.credentials = credentials;
        this.options = options;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    public InstanceOptions getOptions() {
        return options;
    }

    public String toSymbol() {
        final String s = "`:" + host + ":" + port;
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
        return s + "?" + options.toParameters();
    }

    public void updateFrom(KdbInstance instance) {
        setName(instance.getName());
        this.host = instance.host;
        this.port = instance.port;
        this.credentials = instance.credentials;
        this.options = instance.options;

        notifyItemUpdated();
    }

    @Override
    public String toString() {
        return toSymbol();
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

    @Override
    public @NotNull KdbInstance copy() {
        return new KdbInstance(getName(), host, port, credentials, options == null ? null : options.copy());
    }

    @Override
    public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return toTransferableSymbol();
        }
        return super.getTransferData(flavor);
    }

    public static KdbInstance parseInstance(String txt) {
        String url = txt.strip();

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

        return new KdbInstance(txt, split[0], port, credentials, null);
    }
}
