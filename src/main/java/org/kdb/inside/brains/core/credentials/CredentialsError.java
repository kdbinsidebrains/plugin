package org.kdb.inside.brains.core.credentials;

import javax.swing.*;

/**
 * Description of an error returned by {@link CredentialEditor#validateEditor()} method.
 * <p>
 * Each error contains a message cause the error and an optional component.
 *
 * @param message   the error message to be shown in UI
 * @param component the component that caused the error, if known.
 */
public record CredentialsError(String message, JComponent component) {
}
