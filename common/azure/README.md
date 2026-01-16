<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# jclouds Azure Common

Common infrastructure for Azure provider modules (`azureblob`, `azuredatabases`, etc.).

## What's in This Module

- **Credential resolution** using Azure Identity DefaultAzureCredential
- **Session credentials** domain class for handling Azure access tokens with expiration tracking
- **Shared Azure utilities** for all Azure providers

## Azure Identity Dependency

This module declares an **optional** dependency on `com.azure:azure-identity` for credential resolution. This dependency is **not transitively imposed** on applications - you control whether to include it based on your credential needs.

jclouds continues to implement Azure APIs directly without using the Azure SDK for API operations.

## How Credential Resolution Works

### Priority Order

jclouds resolves credentials in the following priority order:

1. **Explicit credentials supplier** - `credentialsSupplier()` builder method
2. **Explicit static credentials** - `credentials()` method, system properties, or overrides
3. **Azure Identity credential chain** - Environment, Managed Identity, CLI, etc. *(Priority 3)*
4. **API metadata defaults** - Default credentials from provider metadata

### The Azure Identity SDK is Optional

**You do NOT need to use Azure Identity features** - jclouds works with traditional credentials:

```java
// Option 1: Explicit static credentials (no Azure Identity used)
BlobStoreContext context = ContextBuilder.newBuilder("azureblob")
    .credentials("myStorageAccount", "myAccountKey")
    .buildView(BlobStoreContext.class);
```

```java
// Option 2: System properties (no Azure Identity used)
// Run with: -Dazureblob.identity=myStorageAccount -Dazureblob.credential=myAccountKey
BlobStoreContext context = ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);
```

### Using Azure Identity Features (Ambient Credentials)

**If you want** Azure Identity credential resolution (Managed Identity, Azure CLI, etc.), you must:

1. **Include Azure Identity dependency** in your application's `pom.xml`:
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.15.1</version>
</dependency>
```

2. **Omit explicit credentials** - Azure Identity will resolve automatically:

```java
// No credentials specified - Azure Identity resolves automatically
BlobStoreContext context = ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);
```

The Azure Identity SDK will check in order:
1. Environment variables: `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`
2. Workload Identity (for AKS with Workload Identity)
3. Managed Identity (for Azure VMs, App Service, Azure Functions, etc.)
4. Azure CLI (if logged in via `az login`)
5. Azure PowerShell (if logged in via `Connect-AzAccount`)
6. Azure Developer CLI (if logged in via `azd auth login`)
7. IntelliJ IDEA (if Azure Toolkit plugin is installed and authenticated)
8. Visual Studio Code (if Azure Account extension is installed and authenticated)

## Supported Credential Sources

### Always Supported (No Azure Identity)
- Explicit credentials via `credentials()` method
- System properties (`-Dazureblob.identity=...`)
- Properties file (`azureblob.identity=...`)

### Supported via Azure Identity (Priority 3)

#### Fully Ambient (No Manual Steps Required)
- **Managed Identity** (System or User-assigned) - Automatic on Azure VMs, App Service, Functions, Container Instances
- **Workload Identity** - Automatic on AKS with Workload Identity configured
- Environment variables (`AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`) - Set by deployment environment

#### Requires Initial Setup
- **Azure CLI** - Requires `az login` before running app
- **Azure PowerShell** - Requires `Connect-AzAccount` before running app
- **Azure Developer CLI** - Requires `azd auth login` before running app
- **IDE integrations** - Must be configured in IntelliJ or VS Code

## Examples

### Example 1: Static Credentials (Works Everywhere)
```java
ContextBuilder.newBuilder("azureblob")
    .credentials("myStorageAccount", "myAccountKey")
    .buildView(BlobStoreContext.class);
```

### Example 2: Environment Variables (Azure Identity)
```bash
export AZURE_TENANT_ID=your-tenant-id
export AZURE_CLIENT_ID=your-client-id
export AZURE_CLIENT_SECRET=your-client-secret
```
```java
ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);  // Credentials from env
```

### Example 3: Managed Identity on Azure VM (Azure Identity)
```java
// Running on Azure VM with Managed Identity enabled
ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);  // Credentials from Managed Identity
```

### Example 4: AKS with Workload Identity (Azure Identity)
```yaml
# Kubernetes ServiceAccount with Workload Identity annotation
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    azure.workload.identity/client-id: "your-client-id"
```
```java
// In pod using service account with Workload Identity
ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);  // Credentials via Workload Identity
```

### Example 5: Azure CLI (Azure Identity)
```bash
# Required: Login before running app
az login

# Optionally set subscription
az account set --subscription "My Subscription"
```
```java
// Azure Identity uses cached CLI credentials
ContextBuilder.newBuilder("azureblob")
    .buildView(BlobStoreContext.class);  // Credentials from Azure CLI
```

**Note:** You must run `az login` periodically when your session expires.

## For Provider Module Developers

To use Azure common infrastructure in your Azure provider:

1. **Add dependency** in your provider's `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.jclouds.common</groupId>
    <artifactId>azure</artifactId>
    <version>${project.version}</version>
</dependency>
```

2. **Use AzureCredentialsProvider** in your API metadata builder:
```java
import org.jclouds.azure.credentials.AzureCredentialsProvider;

public static class Builder extends YourApiMetadata.Builder<YourClient, Builder> {
    protected Builder() {
        super(YourClient.class);
        AzureCredentialsProvider azureCredentialsProvider = new AzureCredentialsProvider();
        id("azure-yourservice")
            .defaultCredentialsSupplier(azureCredentialsProvider.getCredentialsSupplier("https://yourservice.azure.com/.default"));
    }
}
```

## Debugging Credentials

To see which credential source is being used, enable debug logging with the system property:

```bash
java -Djclouds.azure.credentials.debug=true -jar your-app.jar
```

This will output credential resolution details to stderr:
```
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:123]: Retrieving Azure credentials...
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:126]: Successfully created Azure credential provider using default credential chain
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:157]: Requesting Azure access token for scope: https://storage.azure.com/.default
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:161]: Successfully retrieved Azure access token
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:162]: - Token: eyJ0eXAiOiJKV1Qi...
21:30:45 [org.jclouds.azure.credentials.AzureCredentialsProvider:163]: - Expires at: 2026-01-16T22:30:45Z
```

**Note:** Debug output includes partial tokens (first 20 characters). Only enable this for troubleshooting.

## Dependencies

### Required Dependencies
- `org.apache.jclouds:jclouds-core` - Core jclouds components

### Optional Dependencies (for ambient credentials)
- `com.azure:azure-identity` - Azure credential resolution

**Important**: The Azure Identity dependency is marked `<optional>true</optional>` in `common/azure/pom.xml`. It is **not** transitively imposed on your application. If you want ambient credential support (Managed Identity, Azure CLI, etc.), you must explicitly add the dependency to your application's `pom.xml`.

**Why optional?** This prevents version conflicts when your application uses a different Azure SDK version, and reduces dependency footprint for applications using only static credentials.
