/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.azure.credentials;

import com.google.common.base.Supplier;

import org.jclouds.domain.Credentials;
import org.jclouds.azure.domain.SessionCredentials;
import org.jclouds.logging.Logger;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;

/**
 * Azure credentials provider that resolves credentials using the Azure Identity default credentials chain.
 * <p>
 * This class is <b>optional</b> and only used when Azure Identity dependencies are available on the classpath.
 * If Azure Identity is not present, users must provide explicit credentials via {@code credentials()} method.
 * <p>
 * When Azure Identity is available, credentials are resolved through the standard credential provider chain:
 * <ol>
 *   <li>Environment variables (AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET)</li>
 *   <li>Managed Identity (for Azure VMs, App Service, Azure Functions, etc.)</li>
 *   <li>Azure CLI (if logged in via 'az login')</li>
 *   <li>Azure PowerShell (if logged in via 'Connect-AzAccount')</li>
 *   <li>IntelliJ IDEA (if Azure Toolkit plugin is installed and authenticated)</li>
 *   <li>Visual Studio Code (if Azure Account extension is installed and authenticated)</li>
 * </ol>
 * <p>
 * <b>Note on Dependency Injection:</b> This class is typically instantiated in API metadata builders
 * before the Guice injector is created in {@code ContextBuilder.buildInjector()}. Therefore, dependency
 * injection (including {@code @Resource} logger injection) does not work.
 * <p>
 * <b>Debug Logging:</b> By default, logging is disabled. To enable debug logging for credential resolution,
 * set the system property {@code jclouds.azure.credentials.debug=true}:
 * <pre>
 * java -Djclouds.azure.credentials.debug=true -jar your-app.jar
 * </pre>
 * This writes detailed credential resolution information to System.err via {@code Logger.CONSOLE}.
 */
public class AzureCredentialsProvider {

    /**
     * System property to enable debug logging for Azure credential resolution.
     * Set to "true" to enable console logging: -Djclouds.azure.credentials.debug=true
     */
    private static final String DEBUG_PROPERTY = "jclouds.azure.credentials.debug";

    /**
     * Default Azure scope for general Azure Resource Manager operations.
     */
    private static final String DEFAULT_SCOPE = "https://management.azure.com/.default";

    /**
     * Cached check for Azure Identity availability. Determined once at class load time.
     */
    private static final boolean AZURE_IDENTITY_AVAILABLE = checkAzureIdentityAvailable();

    /**
     * Logger for credential operations. Defaults to Logger.NULL (no logging) unless
     * the system property jclouds.azure.credentials.debug=true is set, in which case
     * Logger.CONSOLE is used to write to System.err.
     */
    protected Logger logger = Boolean.getBoolean(DEBUG_PROPERTY) ? Logger.CONSOLE : Logger.NULL;

    // Use Object type to avoid class loading of TokenCredential when Azure Identity is not available
    private Object tokenCredential;

    /**
     * Checks if Azure Identity classes are available on the classpath at runtime.
     * This allows the provider to work with or without Azure Identity dependencies.
     *
     * @return true if Azure Identity is present, false otherwise
     */
    private static boolean checkAzureIdentityAvailable() {
        try {
            Class.forName("com.azure.identity.DefaultAzureCredentialBuilder");
            Class.forName("com.azure.core.credential.TokenCredential");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns whether Azure Identity is available on the classpath.
     * This can be used to conditionally instantiate this provider.
     *
     * @return true if Azure Identity is available, false otherwise
     */
    public static boolean isAzureIdentityAvailable() {
        return AZURE_IDENTITY_AVAILABLE;
    }

    // Use Object type to avoid class loading of TokenCredential when Azure Identity is not available
    protected Object resolveTokenCredential() {
        if (!AZURE_IDENTITY_AVAILABLE) {
            throw new IllegalStateException(
                    "Azure Identity is not available on the classpath. To use ambient credentials (Managed Identity, " +
                    "Azure CLI, Azure PowerShell, etc.), ensure the following dependency is present:\n" +
                    "  - com.azure:azure-identity\n\n" +
                    "Alternatively, provide explicit credentials using the .credentials() method:\n" +
                    "  ContextBuilder.newBuilder(\"azureblob\")\n" +
                    "    .credentials(\"accountName\", \"accountKey\")\n" +
                    "    .buildView(BlobStoreContext.class);");
        }

        try {
            logger.info(Logger.formatWithContext("Retrieving Azure credentials..."));
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();

            logger.info(Logger.formatWithContext("Successfully created Azure credential provider using default credential chain"));
            return credential;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to retrieve Azure credentials.\n" +
                    "Note: Either use Azure credentials configured on your local machine by running 'az login' " +
                    "or 'Connect-AzAccount', or run your program or service in an environment that provides " +
                    "ambient Azure credentials, such as:\n" +
                    "- Azure VM with Managed Identity\n" +
                    "- Azure App Service with Managed Identity\n" +
                    "- Azure Functions with Managed Identity\n" +
                    "- Azure Kubernetes Service (AKS) pod with Managed Identity", e);
        }
    }

    /**
     * Retrieves an access token for the specified scope.
     * <p>
     * Note: No need to manually cache tokens, Azure Identity SDK handles token caching automatically
     * via the DefaultAzureCredential. The SDK's in-memory token cache will return cached tokens
     * when they are still valid and automatically refresh them when needed.
     *
     * @param scope the Azure resource scope (e.g., "https://management.azure.com/.default")
     * @return the access token
     */
    protected AccessToken getAccessToken(String scope) {
        if (this.tokenCredential == null) {
            this.tokenCredential = resolveTokenCredential();
        }

        try {
            logger.info(Logger.formatWithContext("Requesting Azure access token for scope: " + scope));
            TokenRequestContext requestContext = new TokenRequestContext().addScopes(scope);
            AccessToken token = ((TokenCredential) this.tokenCredential).getTokenSync(requestContext);

            logger.info(Logger.formatWithContext("Successfully retrieved Azure access token"));
            logger.info(Logger.formatWithContext("- Token: " + token.getToken().substring(0, Math.min(20, token.getToken().length())) + "..."));
            logger.info(Logger.formatWithContext("- Expires at: " + token.getExpiresAt()));

            return token;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve Azure access token", e);
        }
    }

    public Credentials getCredentials() {
        return getCredentials(DEFAULT_SCOPE);
    }

    /**
     * Returns credentials for the specified Azure scope.
     *
     * @param scope the Azure resource scope (e.g., "https://storage.azure.com/.default")
     * @return credentials containing the access token
     */
    public Credentials getCredentials(String scope) {
        AccessToken token = getAccessToken(scope);

        // Return as SessionCredentials since Azure tokens are temporary
        return SessionCredentials.builder()
                .identity("azure-token")
                .credential(token.getToken())
                .sessionToken(token.getToken())
                .expiration(java.util.Date.from(token.getExpiresAt().toInstant()))
                .build();
    }

    /**
     * Returns a credentials supplier if Azure Identity is available, or null otherwise.
     * <p>
     * This method is designed to be called during API metadata initialization where Azure Identity
     * may or may not be present. If Azure Identity is not available, returns null to allow the
     * credentials priority system to fall back to explicit credentials or other sources.
     * <p>
     * If Azure Identity is available, returns a supplier that lazily resolves credentials using
     * the Azure Identity default credentials chain.
     *
     * @return credentials supplier, or null if Azure Identity is not available
     */
    public Supplier<Credentials> getCredentialsSupplier() {
        if (!AZURE_IDENTITY_AVAILABLE) {
            return null;
        }
        return () -> {
            return getCredentials();
        };
    }

    /**
     * Returns a credentials supplier for a specific scope.
     *
     * @param scope the Azure resource scope
     * @return credentials supplier, or null if Azure Identity is not available
     */
    public Supplier<Credentials> getCredentialsSupplier(String scope) {
        if (!AZURE_IDENTITY_AVAILABLE) {
            return null;
        }
        return () -> {
            return getCredentials(scope);
        };
    }
}
