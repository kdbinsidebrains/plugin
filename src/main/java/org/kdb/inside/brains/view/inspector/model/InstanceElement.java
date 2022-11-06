package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbResult;

public class InstanceElement extends InspectorElement {
    private final KdbResult result;
    private final InstanceConnection connection;

    public InstanceElement(InstanceConnection connection, KdbResult result) {
        super(connection.getCanonicalName(), null, KdbIcons.Node.Instance);
        this.connection = connection;
        this.result = result;
    }

    @Override
    public String getLocationString() {
        return connection.getDetails();
    }

    public KdbResult getResult() {
        return result;
    }

    public InstanceConnection getConnection() {
        return connection;
    }

    @Override
    protected InspectorElement[] buildChildren() {
        return NamespaceElement.buildChildren(null, (Object[]) result.getObject());
    }
}