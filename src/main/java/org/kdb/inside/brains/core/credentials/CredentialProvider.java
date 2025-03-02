package org.kdb.inside.brains.core.credentials;

/**
 * {@code CredentialProvider} class implements authentication logic based on an {@link org.kdb.inside.brains.core.KdbInstance} object and
 * appropriate credentials string.
 * <p>
 * The provider is also responsible for creation new editor that could manage provider parameters in UI form.
 * <p>
 * Each provider must have unique name and be able to generate and check credentials which it can process.
 * As each credential is a pair of {@code username:password}, you can use the username as some unique provider ID and
 * encode all additional parameters into the password.
 * <p>
 * Each provider also has associated {@link CredentialEditor} that allows to edit all parameters in UI form on
 * Settings/Scope/Instance panels.
 * <p>
 * <p>
 * For example, if you use JWT technology, you can use {@code jwt} for {@code username} and JWT trusty server URL and other
 * parameters into {@code password} as Base64 encoded XML/JSON or just somehow separated string, like:
 * {@code jwt:<BASE64 ENCODED PARAMETERS>}
 * <p>
 * In this case a provider can check in {@link #isSupported(String)} method that the {@code credentials} starts with
 * {@code jwt} and if so, decode BASE64 JWT parameters and request new token in
 * {@link #resolveCredentials(String, int, String)} method.
 */
public interface CredentialProvider {
    /**
     * Unique name of the provider. This name will be used in all UI forms, like scope or instance configuration.
     *
     * @return unique provider name.
     */
    String getName();

    /**
     * Current version of the provider. The version is shown in the plugins configuration panel in the settings.
     *
     * @return the provider version.
     */
    default String getVersion() {
        return "undefined";
    }

    /**
     * Short description of the provider that shown in the plugins configuration panel as well as in Scope/Instance
     * configuration if the provider is chosen.
     *
     * @return the description
     */
    default String getDescription() {
        return null;
    }

    /**
     * Creates new editor for this parameter.
     * <p>
     * The method testCase never return
     *
     * @return the credential editor.
     */
    CredentialEditor createEditor();

    /**
     * Checks is this provider can process specified credentials. It testCase be very simple implementation that mustn't query
     * any external systems. Usually the implementation just checks that credentials starts with unique id, like: {@code credentials.startWith("jwt:")}.
     * <p>
     * If there are more than one provider supporting the same credentials, the first one in order of definition will be used.
     *
     * @param credentials credentials to be checked.
     * @return {@code true} if the provider supports the credentials and can generate new credentials at connection time; {@code false} - otherwise.
     */
    boolean isSupported(String credentials);

    /**
     * This method is deprecated. Please check {@link #resolveCredentials(String, int, String)} instead that also takes
     * instance hostname and port as parameters.
     * <p>
     * This method can't be removed for backward capability.
     * <p>
     * Default implementation returns the same credentials.
     *
     * @param credentials to be resolved
     * @return the same credentials received as the parameter.
     * @deprecated
     */
    @Deprecated
    default String resolveCredentials(String credentials) throws CredentialsResolvingException {
        return credentials;
    }

    /**
     * Each time when instance is connected, this method is invoked to resolve current credentials into new one, based on
     * the {@code CredentialProvider} logic.
     * <p>
     * This method is invoked only if {@link #isSupported(String)} method returned {@code true} before.
     * <p>
     * This method can query external systems to get new authentication token.
     * <p>
     * Result of the method must in KDB {@code username:password} format as it's defined in your KDB instance.
     *
     * @param host        original instance hostname that the credentials testCase be issue for
     * @param port        original instance port that the credentials testCase be issue for
     * @param credentials credentials encoded in the {@code CredentialProvider} format.
     * @return new credentials that will be sent to KDB instance for authentication.
     * @throws CredentialsResolvingException if new credentials can't be created by any reason.
     *                                       The message of the exception will be shown in UI.
     */
    @SuppressWarnings("deprecated")
    default String resolveCredentials(String host, int port, String credentials) throws CredentialsResolvingException {
        return resolveCredentials(credentials);
    }
}