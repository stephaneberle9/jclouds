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
# jclouds AWS S3 Provider

Amazon S3 (Simple Storage Service) implementation using the jclouds BlobStore API.

## Features

- Full BlobStore API implementation for Amazon S3
- Support for all S3 storage classes (Standard, IA, Glacier, etc.)
- Multipart upload for large objects
- Server-side encryption support
- Bucket lifecycle policies
- Cross-region replication
- AWS credential resolution via AWS SDK (IAM roles, SSO, etc.)

## Authentication

This provider uses the [jclouds AWS Common](../../common/aws/README.md) infrastructure for credential and region resolution.

### Quick Start - Static Credentials

```java
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .credentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
    .buildView(BlobStoreContext.class);

BlobStore blobStore = context.getBlobStore();
```

### Ambient Credentials (IAM Roles, SSO, etc.)

For AWS SDK-based credential resolution (EC2 instance roles, EKS IRSA, AWS SSO, etc.), see the [AWS Common README](../../common/aws/README.md#using-aws-sdk-features-ambient-credentials) for detailed setup instructions.

```java
// No credentials specified - uses AWS SDK credential chain
// Requires AWS SDK dependencies in your pom.xml (see AWS Common README)
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .buildView(BlobStoreContext.class);
```

## Usage Examples

### Basic Operations

```java
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .credentials(accessKeyId, secretAccessKey)
    .buildView(BlobStoreContext.class);

BlobStore blobStore = context.getBlobStore();

// Create a bucket (container)
blobStore.createContainerInLocation(null, "my-bucket");

// Upload a blob
Blob blob = blobStore.blobBuilder("my-file.txt")
    .payload("Hello, S3!")
    .build();
blobStore.putBlob("my-bucket", blob);

// Download a blob
Blob retrieved = blobStore.getBlob("my-bucket", "my-file.txt");
String content = Strings2.toStringAndClose(retrieved.getPayload().openStream());

// List blobs in a bucket
PageSet<? extends StorageMetadata> list = blobStore.list("my-bucket");

// Delete a blob
blobStore.removeBlob("my-bucket", "my-file.txt");

// Delete a bucket
blobStore.deleteContainer("my-bucket");

context.close();
```

### Multipart Upload for Large Files

```java
BlobStore blobStore = context.getBlobStore();

// Multipart upload is automatic for files > 5MB
File largeFile = new File("/path/to/large/file.zip");
Blob blob = blobStore.blobBuilder("large-file.zip")
    .payload(largeFile)
    .build();

blobStore.putBlob("my-bucket", blob, multipart());
```

### Server-Side Encryption

```java
import static org.jclouds.s3.domain.S3Object.Metadata.SERVER_SIDE_ENCRYPTION;

Blob blob = blobStore.blobBuilder("encrypted-file.txt")
    .payload("Sensitive data")
    .userMetadata(ImmutableMap.of(SERVER_SIDE_ENCRYPTION, "AES256"))
    .build();

blobStore.putBlob("my-bucket", blob);
```

### Setting Storage Class

```java
import org.jclouds.s3.domain.CannedAccessPolicy;
import static org.jclouds.s3.reference.S3Constants.PROPERTY_S3_STORAGE_CLASS;

// Use Infrequent Access storage class
Blob blob = blobStore.blobBuilder("archive-file.txt")
    .payload("Archived data")
    .userMetadata(ImmutableMap.of(PROPERTY_S3_STORAGE_CLASS, "STANDARD_IA"))
    .build();

blobStore.putBlob("my-bucket", blob);
```

### Using Specific Region

```java
// Specify region explicitly
BlobStoreContext context = ContextBuilder.newBuilder("aws-s3")
    .credentials(accessKeyId, secretAccessKey)
    .overrides(Properties.builder()
        .property("jclouds.region", "us-west-2")
        .build())
    .buildView(BlobStoreContext.class);

// Or use system property: -Daws-s3.region=us-west-2
```

See the [AWS Common README](../../common/aws/README.md#region-detection) for automatic region detection options.

## Configuration Properties

Key properties you can override:

| Property | Description | Default |
|----------|-------------|---------|
| `jclouds.region` | AWS region to use | Auto-detected |
| `jclouds.s3.virtual-host-buckets` | Use virtual-host style URLs | `true` |
| `jclouds.max-connections-per-context` | Max HTTP connections | `20` |
| `jclouds.s3.service-path` | Service path (for S3-compatible services) | `/` |

## Maven Dependency

```xml
<dependency>
    <groupId>org.apache.jclouds.provider</groupId>
    <artifactId>aws-s3</artifactId>
    <version>2.9.0</version>
</dependency>

<!-- For ambient credential support (IAM roles, SSO, etc.) -->
<!-- See common/aws/README.md for required AWS SDK dependencies -->
```

## Running Live Tests

Test against real AWS S3 with explicit credentials:

```sh
mvn clean install -Plive \
  -pl :aws-s3 \
  -Dtest=AWSS3ClientLiveTest \
  -Dtest.aws-s3.identity=<aws_access_key_id> \
  -Dtest.aws-s3.credential=<aws_secret_access_key>
```

With session token (for temporary credentials):

```sh
mvn clean install -Plive \
  -pl :aws-s3 \
  -Dtest=AWSS3ClientLiveTest \
  -Dtest.aws-s3.identity=<aws_access_key_id> \
  -Dtest.aws-s3.credential=<aws_secret_access_key> \
  -Dtest.aws-s3.sessionToken=<aws_session_token>
```

Or use ambient credentials (requires AWS SDK dependencies):

```sh
# Uses AWS SDK credential chain (env vars, IAM roles, SSO, etc.)
mvn clean install -Plive -pl :aws-s3 -Dtest=AWSS3ClientLiveTest
```

## Troubleshooting

### Enable Debug Logging for Credentials

```bash
java -Djclouds.aws.credentials.debug=true -jar your-app.jar
```

See [AWS Common README - Debugging Credentials](../../common/aws/README.md#debugging-credentials) for details.

### Common Issues

**Issue**: `NoSuchMethodError` or credential resolution failures
**Solution**: Ensure all AWS SDK dependencies are included. See [AWS Common README](../../common/aws/README.md#dependencies).

**Issue**: Bucket name DNS compliance errors
**Solution**: Use lowercase letters, numbers, and hyphens only. Bucket names must be DNS-compliant.

**Issue**: 403 Forbidden errors
**Solution**: Verify your IAM policy includes necessary S3 permissions (`s3:GetObject`, `s3:PutObject`, etc.).

## Additional Resources

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [jclouds BlobStore Guide](https://jclouds.apache.org/guides/blobstore/)
- [AWS Common README](../../common/aws/README.md) - Credential configuration
- [S3 API Reference](https://jclouds.apache.org/reference/javadoc/)
