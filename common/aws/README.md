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
# jclouds AWS Common

Common infrastructure for AWS provider modules (`aws-s3`, `aws-ec2`, `aws-sqs`, etc.).

## What's in This Module

- **Credential resolution** using AWS SDK v2 DefaultCredentialsProvider
- **Region detection** using AWS SDK v2 DefaultAwsRegionProviderChain
- **Shared AWS utilities** for all AWS providers

## AWS SDK Dependency

This module declares **optional** dependencies on `software.amazon.awssdk` for credential and region resolution. These dependencies are **not transitively imposed** on applications - you control whether to include them based on your credential needs.

jclouds continues to implement AWS APIs directly without using the AWS SDK for API operations.

See [ADR-001](ADR-001-aws-sdk-for-credentials.md) for the architectural decision rationale.

## How Credential Resolution Works

### Priority Order

jclouds resolves credentials in the following priority order:

1. **Explicit credentials supplier** - `credentialsSupplier()` builder method
2. **Explicit static credentials** - `credentials()` method, system properties, or overrides
3. **AWS SDK credential chain** - Environment, files, IAM roles, SSO, etc. *(Priority 3)*
4. **API metadata defaults** - Default credentials from provider metadata

### The AWS SDK is Optional

**You do NOT need to use AWS SDK features** - jclouds works with traditional credentials:

```java
// Option 1: Explicit static credentials (no AWS SDK used)
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .credentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
    .buildView(BlobStoreContext.class);
```

```java
// Option 2: System properties (no AWS SDK used)
// Run with: -Daws-s3.identity=AKIA... -Daws-s3.credential=wJalr...
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);
```

### Using AWS SDK Features (Ambient Credentials)

**If you want** AWS SDK credential resolution (env vars, IAM roles, SSO, etc.), you must:

1. **Include AWS SDK dependencies** in your application's `pom.xml`:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>auth</artifactId>
    <version>2.36.2</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>regions</artifactId>
    <version>2.36.2</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sts</artifactId>
    <version>2.36.2</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sso</artifactId>
    <version>2.36.2</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ssooidc</artifactId>
    <version>2.36.2</version>
</dependency>
```

2. **Omit explicit credentials** - AWS SDK will resolve automatically:

```java
// No credentials specified - AWS SDK resolves automatically
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);
```

The AWS SDK will check in order:
1. Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`
2. System properties: `aws.accessKeyId`, `aws.secretKey`
3. Web Identity Token (for EKS/IRSA)
4. Shared credentials file: `~/.aws/credentials`
5. Shared configuration file: `~/.aws/config`
6. SSO credentials (AWS IAM Identity Center)
7. ECS container credentials
8. EC2 instance metadata (IMDSv2)

## Supported Credential Sources

### ✅ Always Supported (No AWS SDK)
- Explicit credentials via `credentials()` method
- System properties (`-Daws-s3.identity=...`)
- Properties file (`aws-s3.identity=...`)

### ✅ Supported via AWS SDK (Priority 3)

#### Fully Ambient (No Manual Steps Required)
- **EC2 instance metadata** (IAM instance roles) - Automatic on EC2
- **ECS task roles** (IAM roles for tasks) - Automatic on ECS
- **EKS/IRSA** (IAM Roles for Service Accounts) - Automatic on EKS with service account
- Environment variables (`AWS_ACCESS_KEY_ID`, etc.) - Set by deployment environment

#### Requires Initial Setup
- **AWS SSO** (AWS IAM Identity Center) - Requires `aws sso login` before running app
- `~/.aws/credentials` and `~/.aws/config` files - Must be configured on machine
- **Credential process** (external credential programs) - Must configure process in `~/.aws/config`

## Examples

### Example 1: Static Credentials (Works Everywhere)
```java
ContextBuilder.newBuilder("aws-s3")
    .credentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtn...")
    .buildView(BlobStoreContext.class);
```

### Example 2: Environment Variables (AWS SDK)
```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtn...
```
```java
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);  // Credentials from env
```

### Example 3: EC2 Instance Role (AWS SDK)
```java
// Running on EC2 with IAM instance role attached
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);  // Credentials from instance metadata
```

### Example 4: EKS with IRSA (AWS SDK)
```yaml
# Kubernetes ServiceAccount with IAM role annotation
apiVersion: v1
kind: ServiceAccount
metadata:
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/my-app-role
```
```java
// In pod using service account
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);  // Credentials via IRSA
```

### Example 5: AWS SSO (AWS SDK)
```bash
# One-time: Configure SSO profile
aws configure sso

# Required: Login before running app (session typically lasts 8-12 hours)
aws sso login --profile my-sso-profile

# Set profile for your app
export AWS_PROFILE=my-sso-profile
```
```java
// AWS SDK auto-refreshes credentials until SSO session expires
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);  // Credentials from SSO
```

**Note:** You must run `aws sso login` periodically when your SSO session expires. Credentials auto-refresh within the session, but the initial login is manual.

### Example 6: AWS Profile (AWS SDK)
```ini
# ~/.aws/credentials
[production]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtn...
```
```bash
export AWS_PROFILE=production
```
```java
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);  // Credentials from profile
```

## Region Detection

Region is automatically detected from:
1. System property: `aws-s3.region` (or provider-specific property)
2. AWS SDK DefaultAwsRegionProviderChain:
   - Environment variable: `AWS_REGION`
   - `~/.aws/config` file
   - EC2 instance metadata
   - Default: `us-east-1`

## For Provider Module Developers

To use AWS common infrastructure in your AWS provider:

1. **Add dependency** in your provider's `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.jclouds.common</groupId>
    <artifactId>aws</artifactId>
    <version>${project.version}</version>
</dependency>
```

2. **Use AWSCredentialsProvider** in your API metadata builder:
```java
import org.jclouds.aws.credentials.AWSCredentialsProvider;

public static class Builder extends YourApiMetadata.Builder<YourClient, Builder> {
    private final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProvider();

    protected Builder() {
        super(YourClient.class);
        id("aws-yourservice")
            .defaultCredentialsSupplier(awsCredentialsProvider.getCredentialsSupplier())
            .defaultProperties(defaultProperties(awsCredentialsProvider.getRegion()));
    }
}
```

## Debugging Credentials

To see which credential source is being used, enable debug logging with the system property:

```bash
java -Djclouds.aws.credentials.debug=true -jar your-app.jar
```

This will output credential resolution details to stderr:
```
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:149]: Retrieving AWS credentials...
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:121]: Successfully retrieved temporary AWS credentials from default credentials provider chain
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:127]: - Access Key ID: ASIATEMP...
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:128]: - Secret Access Key: wJalrXUt...
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:132]: - Session Token: IQoJb3Jp...
21:30:45 [org.jclouds.aws.credentials.AWSCredentialsProvider:134]: - Region: us-west-2
```

**Note:** Debug output includes partial credentials (first 8 characters). Only enable this for troubleshooting.

## Dependencies

### Required Dependencies
- `org.apache.jclouds.api:sts` - AWS domain classes (SessionCredentials)

### Optional Dependencies (for ambient credentials)
- `software.amazon.awssdk:auth` - Core credential resolution (~250 KB)
- `software.amazon.awssdk:regions` - Region detection (~200 KB)
- `software.amazon.awssdk:sts` - STS integration for temporary credentials
- `software.amazon.awssdk:sso` - AWS SSO integration
- `software.amazon.awssdk:ssooidc` - SSO OIDC support

**Important**: AWS SDK dependencies are marked `<optional>true</optional>` in `common/aws/pom.xml`. They are **not** transitively imposed on your application. If you want ambient credential support (EC2 roles, EKS IRSA, SSO, etc.), you must explicitly add **all five** dependencies to your application's `pom.xml` - experiments have shown that just including `auth` and `regions` is not sufficient for full ambient credential functionality.

**Why optional?** This prevents version conflicts when your application uses a different AWS SDK version, and reduces dependency footprint for applications using only static credentials.
