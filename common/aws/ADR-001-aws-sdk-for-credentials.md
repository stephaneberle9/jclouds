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
# ADR-001: Use AWS SDK v2 for Credential Resolution

## Status
Accepted

## Context

jclouds has historically avoided dependencies on cloud vendor SDKs, instead implementing cloud APIs directly using HTTP clients and protocol-level libraries. This allows jclouds to:
- Provide a unified multi-cloud abstraction
- Avoid version conflicts with user applications
- Maintain full control over API implementation
- Keep dependencies minimal

However, AWS credential resolution has become increasingly complex:

1. **Multiple credential sources** (11+ as of 2024):

   **Fully ambient (no manual steps):**
   - EC2 instance metadata (IMDSv2 with session tokens) - automatic on EC2 with IAM role
   - ECS container credentials - automatic on ECS with task role
   - Web Identity Token (for EKS/IRSA - Kubernetes IAM Roles for Service Accounts) - automatic in EKS pods
   - Environment variables (`AWS_ACCESS_KEY_ID`, etc.) - set by deployment environment

   **Require initial setup:**
   - SSO credentials (AWS IAM Identity Center) - requires `aws sso login` before app runs, but then auto-refreshes
   - Shared credentials file (`~/.aws/credentials`) - must be configured on machine
   - Shared configuration file (`~/.aws/config`) - must be configured on machine
   - Java system properties - must be set by application startup
   - Credential process - must configure external process in `~/.aws/config`
   - Custom credential providers
   - Future sources AWS may add

2. **Complex protocols**:
   - **SSO**: Browser-based OAuth flow, token caching, automatic refresh
   - **IRSA**: Web Identity Federation requiring STS AssumeRoleWithWebIdentity calls
   - **IMDSv2**: Session-token-based EC2 metadata service v2
   - **Credential refresh**: Temporary credentials expire and need automatic renewal

3. **Security-critical code**:
   - Credential handling requires careful implementation to avoid vulnerabilities
   - AWS SDK is audited and tested at massive scale
   - Edge cases in credential resolution can lead to auth failures or security issues

Reimplementing this would require approximately 1,500-2,000 lines of security-sensitive code that we would need to maintain, debug, and keep up-to-date with AWS changes.

## Decision

We will use **AWS SDK v2 (`software.amazon.awssdk`)** specifically for credential and region resolution in the `common/aws` module, while continuing to use jclouds' own HTTP-based implementation for all AWS API calls.

### Scope of AWS SDK Usage

**What we USE AWS SDK for:**
- ✅ `DefaultCredentialsProvider` - Credential chain resolution
- ✅ `DefaultAwsRegionProviderChain` - Region detection
- ✅ Automatic credential refresh for temporary credentials

**What we DO NOT use AWS SDK for:**
- ❌ S3 API operations (jclouds implements these directly)
- ❌ EC2 API operations (jclouds implements these directly)
- ❌ Any other AWS service API calls
- ❌ Request signing (jclouds implements AWS Signature v4)
- ❌ Response parsing (jclouds uses its own XML/JSON parsers)

### Isolation

The AWS SDK dependency is isolated in the `common/aws` module:
```
common/aws/
├── pom.xml                    # AWS SDK dependencies here
└── src/main/java/org/jclouds/aws/
    └── credentials/
        └── AWSCredentialsProvider.java
```

Only AWS-specific provider modules (`providers/aws-s3`, `providers/aws-ec2`, etc.) depend on `common/aws`. Other cloud providers (Google Cloud, Azure, OpenStack) are unaffected.

### Dependency Size

- `software.amazon.awssdk:auth` - Core credential resolution (~250 KB)
- `software.amazon.awssdk:regions` - Region detection (~200 KB)
- Total: ~500 KB additional dependencies

Using the AWS SDK BOM ensures version consistency and prevents conflicts.

## Consequences

### Positive

1. **Full AWS credential support**:
   - Users get all 11+ credential sources automatically
   - SSO and IRSA (Kubernetes) support work out of the box
   - Future AWS credential sources are supported automatically

2. **Security**:
   - AWS-maintained, audited code for credential handling
   - Security fixes come from AWS
   - Lower risk of credential handling bugs

3. **Maintenance**:
   - No need to maintain complex credential resolution code
   - No need to track AWS changes to credential mechanisms
   - Reduces long-term maintenance burden

4. **User expectations**:
   - Credentials work exactly like AWS CLI/SDK
   - Standard AWS configuration files work unchanged
   - No surprising credential resolution differences

5. **Optional**:
   - Users can still provide static credentials directly
   - AWS SDK is only used when no explicit credentials provided
   - Credentials priority (see ContextBuilder.java:336-365):
     1. `credentialsSupplier()` builder method
     2. `credentials()` builder method or overrides
     3. **AWS SDK DefaultCredentialsProvider** (Priority 3)
     4. API metadata static defaults

### Negative

1. **Breaks jclouds precedent**:
   - First time jclouds depends on a cloud vendor SDK
   - Sets a precedent that might be questioned for other clouds

2. **Optional dependencies require user action**:
   - Users wanting ambient credentials must explicitly add AWS SDK to their pom.xml
   - Not "zero configuration" for EC2/EKS/ECS deployments
   - Requires understanding which dependencies to add

3. **Documentation burden**:
   - Must clearly document when AWS SDK is needed
   - Must provide clear error messages when SDK is missing but needed

### Mitigation Strategies

1. **Keep scope limited**: Only use AWS SDK for credentials/region, never for API calls
2. **BOM management**: Use `software.amazon.awssdk:bom` for version consistency
3. **Document clearly**: Make it clear in documentation that AWS SDK is used only for auth
4. **Monitor alternatives**: If jclouds-specific needs arise, can still reimplement specific sources
5. **Optional usage**: Credentials priority system means AWS SDK is only used as fallback

## Alternatives Considered

### 1. Reimplement All Credential Sources
- **Pros**: No AWS SDK dependency, full jclouds control
- **Cons**: 1,500-2,000 lines of security-critical code, ongoing maintenance, likely missing edge cases
- **Decision**: Rejected due to complexity and security risk

### 2. Reimplement Only Core Sources (Env, Files, EC2 Metadata)
- **Pros**: Reduced dependency, simpler implementation (~400-500 lines)
- **Cons**: No SSO or IRSA support, which are the primary use cases for this feature
- **Decision**: Rejected because it doesn't solve the user's problem

### 3. Require External Credential Resolution
- **Pros**: No code needed, users run `aws sso login` externally
- **Cons**: Terrible UX, credentials expire, doesn't work in EKS pods
- **Decision**: Rejected due to poor user experience

### 4. Use AWS SDK v1 Instead of v2
- **Pros**: More widely used, longer history
- **Cons**: AWS SDK v1 is in maintenance mode, v2 is the future, larger dependencies
- **Decision**: Rejected in favor of forward-looking v2

### 5. Make AWS SDK Dependencies Optional (Maven `optional` Scope)
- **Pros**:
  - Users who don't need IRSA/SSO don't get AWS SDK dependencies
  - More explicit opt-in model
  - Reduces transitive dependency bloat for static credential users
  - **Avoids version conflicts** when application uses different AWS SDK version
  - **Application controls dependency version** - essential when app needs specific AWS SDK version
- **Cons**:
  - Primary use case (IRSA on EKS) doesn't work out of the box
  - Users must explicitly add AWS SDK to their pom.xml
  - Requires documentation and clear error messages
  - More configuration burden on users
- **Decision**: **Accepted**
  - Version conflicts are more problematic than explicit configuration
  - Applications using AWS SDK features typically already have AWS SDK dependencies
  - Runtime detection and graceful fallback already implemented
  - Clear error messages guide users when SDK is needed but missing
  - Better separation: jclouds provides integration, apps control SDK version

## Maven Scope Decision

The AWS SDK dependencies in `common/aws` use **optional scope**, meaning they are NOT transitive to applications:

```xml
<dependencies>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>auth</artifactId>
    <optional>true</optional>
  </dependency>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>regions</artifactId>
    <optional>true</optional>
  </dependency>
</dependencies>
```

**Rationale:**
- **Prevents version conflicts**: Applications often have their own AWS SDK version requirements
- **Application controls SDK version**: App chooses which AWS SDK version to use (older or newer)
- **Reduces forced dependencies**: Apps using only static credentials don't get AWS SDK transitively
- **Better separation**: jclouds provides integration; applications control dependencies
- **Runtime detection works**: Code uses `Class.forName()` to detect AWS SDK availability

**Trade-off accepted:** Applications wanting ambient credentials must explicitly add AWS SDK dependencies. However:
- Most AWS-using applications already have AWS SDK dependencies
- Explicit dependency is better than forcing a potentially conflicting version
- Clear error messages guide users when SDK is needed but missing

### Using AWS SDK Dependencies (Optional)

AWS SDK dependencies are marked `<optional>true</optional>` in `common/aws/pom.xml`. They are NOT transitively included in your application. If you want ambient credential support, you must explicitly add them.

**How it works:**
1. **Runtime detection** - `AWSCredentialsProvider.isAwsSdkAvailable()` checks if AWS SDK classes are present
2. **Lazy initialization** - Provider metadata only instantiates `AWSCredentialsProvider` if AWS SDK is available
3. **Graceful fallback** - Region defaults to `us-east-1` if AWS SDK not present
4. **Clear errors** - If ambient credentials are requested but AWS SDK missing, user gets helpful error message

#### Including AWS SDK in Your Application

To use ambient credentials (EC2 roles, EKS IRSA, SSO, etc.), add AWS SDK dependencies to your `pom.xml`:

```xml
<!-- In your application pom.xml -->
<dependencies>
  <dependency>
    <groupId>org.apache.jclouds.provider</groupId>
    <artifactId>aws-s3</artifactId>
    <version>2.7.1-SNAPSHOT</version>
  </dependency>

  <!-- Add AWS SDK for ambient credential support -->
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>auth</artifactId>
    <version>2.36.2</version>  <!-- Or any version you prefer -->
  </dependency>
  <dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>regions</artifactId>
    <version>2.36.2</version>  <!-- Or any version you prefer -->
  </dependency>
</dependencies>
```

**Key benefit**: You control the AWS SDK version, avoiding conflicts if your application uses other AWS SDK modules.

#### Without AWS SDK Dependencies

**What works:**
```java
// ✅ Explicit credentials work fine
ContextBuilder.newBuilder("aws-s3")
    .credentials("AKIAIOSFODNN7EXAMPLE", "wJalr...")
    .buildView(BlobStoreContext.class);

// ✅ Region defaults to us-east-1, or can be overridden
ContextBuilder.newBuilder("aws-s3")
    .credentials("AKIAIOSFODNN7EXAMPLE", "wJalr...")
    .overrides(new Properties() {{
        setProperty("jclouds.region", "us-west-2");
    }})
    .buildView(BlobStoreContext.class);
```

**What fails:**
```java
// ❌ Ambient credentials fail with clear error
ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);

// Error: "AWS SDK v2 is not available on the classpath. To use ambient
// credentials (EC2 instance roles, ECS task roles, EKS/IRSA, SSO, etc.),
// ensure the following dependencies are present:
//   - software.amazon.awssdk:auth
//   - software.amazon.awssdk:regions
//
// Alternatively, provide explicit credentials using the .credentials() method"
```

**Use cases for omitting AWS SDK:**
- Applications that only use explicit static credentials
- Minimizing dependency footprint in size-constrained environments (saves ~500KB)
- Letting application control AWS SDK version to avoid conflicts
- Deploying to environments where ambient credentials are not available

**Implementation details:**
- `AWSCredentialsProvider` uses `Class.forName()` to detect AWS SDK classes at class-load time
- `AWSS3ApiMetadata.Builder` conditionally instantiates provider based on availability check
- Region detection gracefully falls back to `us-east-1` if AWS SDK not present
- Clear error messages guide users when AWS SDK is needed but missing

## References

- [jclouds CLAUDE.md](../../CLAUDE.md) - Project architecture guidance
- [ContextBuilder.java](../../core/src/main/java/org/jclouds/ContextBuilder.java) - Credentials priority implementation
- [AWS SDK v2 Credentials Documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)
- [AWS SDK v2 Auth Module](https://github.com/aws/aws-sdk-java-v2/tree/master/core/auth)

## Notes

This decision applies specifically to AWS and should not be interpreted as a general policy for all cloud providers. Each cloud provider's credential mechanisms should be evaluated independently. The decision here is based on the unique complexity and security requirements of AWS credential resolution.
