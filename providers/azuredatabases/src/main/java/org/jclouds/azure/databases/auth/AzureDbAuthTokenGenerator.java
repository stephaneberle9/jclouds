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
package org.jclouds.azure.databases.auth;

import org.jclouds.datasource.auth.DbAuthTokenGenerator;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ChainedTokenCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredentialBuilder;

/**
 * Generates authentication tokens for connecting to an Azure Database using Entra ID credentials.
 *
 * <p>This utility class creates authentication tokens that can be used instead
 * of passwords when connecting to Azure Databases for PostgreSQL/MySQL with Entra ID authentication enabled.
 *
 * <p>The tokens are typically valid for 1 hour and should be regenerated for each
 * new connection.
 *
 * <p>Azure credentials are obtained using DefaultAzureCredential which supports multiple
 * authentication methods in order:
 * <ul>
 *   <li>Environment variables (AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET)</li>
 *   <li>Workload Identity (for AKS with federated tokens)</li>
 *   <li>Managed Identity (for Azure VMs, App Service, Container Instances, etc.)</li>
 *   <li>Azure CLI (az login)</li>
 *   <li>Azure PowerShell</li>
 *   <li>IntelliJ, VS Code, etc.</li>
 * </ul>
 */
public class AzureDbAuthTokenGenerator implements DbAuthTokenGenerator {

   private static final String AZURE_OSSRDBMS_SCOPE = "https://ossrdbms-aad.database.windows.net/.default";

   private final TokenCredential credential;

   /**
    * Creates a new token generator using a chain of Azure credentials.
    * The chain tries WorkloadIdentityCredential first (for AKS), then falls back to DefaultAzureCredential.
    *
    * @return a new AzureDbAuthTokenGenerator instance
    */
   public static AzureDbAuthTokenGenerator create() {
      // Build a credential chain that tries Workload Identity first, then falls back to DefaultAzureCredential
      // Only include WorkloadIdentityCredential if the required environment variables are set
      ChainedTokenCredentialBuilder builder = new ChainedTokenCredentialBuilder();

      // Check if Workload Identity environment variables are present
      String clientId = System.getenv("AZURE_CLIENT_ID");
      String tenantId = System.getenv("AZURE_TENANT_ID");
      String tokenFile = System.getenv("AZURE_FEDERATED_TOKEN_FILE");

      if (clientId != null && tenantId != null && tokenFile != null) {
         builder.addLast(new WorkloadIdentityCredentialBuilder().build());
      }

      builder.addLast(new DefaultAzureCredentialBuilder().build());

      TokenCredential credential = builder.build();
      return new AzureDbAuthTokenGenerator(credential);
   }

   /**
    * Creates a new token generator with a specific credential.
    *
    * @param credential Azure credential provider
    */
   public AzureDbAuthTokenGenerator(TokenCredential credential) {
      this.credential = credential;
   }

   /**
    * Generates an authentication token for connecting to an Azure Database using Entra ID credentials.
    *
    * <p>The token is obtained by requesting an access token for the Azure OSS RDBMS scope
    * (https://ossrdbms-aad.database.windows.net/.default) which is the required scope
    * for Azure Databases for PostgreSQL and MySQL Entra ID authentication.
    *
    * @return authentication token valid for approximately 1 hour
    */
   @Override
   public String generateToken() {
      TokenRequestContext request = new TokenRequestContext().addScopes(AZURE_OSSRDBMS_SCOPE);
      return credential.getTokenSync(request).getToken();
   }
}
