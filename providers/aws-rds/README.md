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
# jclouds AWS RDS Provider

Amazon RDS (Relational Database Service) DataSource implementation with support for IAM database authentication.

## Features

- **DataSource abstraction** - Standard JDBC DataSource interface
- **HikariCP connection pooling** - High-performance JDBC connection pool
- **IAM database authentication** - Passwordless authentication using AWS IAM
- **Automatic token refresh** - IAM auth tokens auto-regenerate (15-minute validity)
- **Static password support** - Traditional username/password authentication
- **AWS credential resolution** - Ambient credentials via AWS SDK (IAM roles, SSO, etc.)

## Authentication

This provider supports two authentication modes:

### 1. Traditional Password Authentication

Standard database authentication with username and password:

```java
DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", "mypassword")  // Static password
    .buildView(DataSourceContext.class);

DataSource dataSource = context.getDataSource();
```

### 2. IAM Database Authentication

Passwordless authentication using AWS IAM credentials. Requires:
- Database user configured for IAM authentication
- IAM policy granting `rds-db:connect` permission
- AWS credentials (see [AWS Common README](../../common/aws/README.md))

```java
DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", "")  // Empty password triggers IAM auth
    .buildView(DataSourceContext.class);

DataSource dataSource = context.getDataSource();
```

**How it works:**
1. Empty or null password automatically enables IAM authentication mode
2. Authentication tokens are generated on-demand when connections are requested
3. Tokens are valid for 15 minutes and automatically refresh
4. Uses AWS SDK credential chain (see [AWS Common README](../../common/aws/README.md))

## AWS Credential Configuration

For IAM database authentication, this provider uses the [jclouds AWS Common](../../common/aws/README.md) infrastructure for AWS credential resolution.

### Required AWS SDK Dependencies for IAM Auth

Add these dependencies to your `pom.xml`:

```xml
<!-- AWS SDK dependencies for IAM authentication -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>rds</artifactId>
    <version>2.36.2</version>
</dependency>
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

<!-- For ambient credentials (EC2 roles, EKS IRSA, SSO) -->
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

See [AWS Common README - Dependencies](../../common/aws/README.md#dependencies) for details.

## Usage Examples

### Example 1: Static Password Authentication

```java
import org.jclouds.ContextBuilder;
import org.jclouds.datasource.DataSourceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("admin", "MySecurePassword123!")
    .buildView(DataSourceContext.class);

DataSource dataSource = context.getDataSource();

try (Connection conn = dataSource.getConnection()) {
    ResultSet rs = conn.createStatement().executeQuery("SELECT version()");
    while (rs.next()) {
        System.out.println("MySQL version: " + rs.getString(1));
    }
}

context.close();
```

### Example 2: IAM Database Authentication

```java
// Empty password triggers IAM authentication
DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", "")  // Empty password = IAM auth
    .buildView(DataSourceContext.class);

DataSource dataSource = context.getDataSource();

// Tokens are automatically generated and refreshed
try (Connection conn = dataSource.getConnection()) {
    // Use connection normally
    ResultSet rs = conn.createStatement().executeQuery("SELECT CURRENT_USER()");
    while (rs.next()) {
        System.out.println("Connected as: " + rs.getString(1));
    }
}
```

### Example 3: IAM Auth with EC2 Instance Role

```java
// Running on EC2 with IAM instance role attached
// No AWS credentials needed - automatically resolved from instance metadata
DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", null)  // Null password = IAM auth
    .buildView(DataSourceContext.class);

DataSource dataSource = context.getDataSource();

// Connection pool automatically manages token generation/refresh
try (Connection conn = dataSource.getConnection()) {
    // Connection authenticated via IAM role
}
```

### Example 4: IAM Auth with AWS SSO

```bash
# One-time: Configure SSO
aws configure sso

# Required: Login before running app
aws sso login --profile my-sso-profile

# Set profile for your app
export AWS_PROFILE=my-sso-profile
```

```java
DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", "")  // IAM auth
    .buildView(DataSourceContext.class);

// AWS SDK auto-resolves credentials from SSO profile
DataSource dataSource = context.getDataSource();
```

See [AWS Common README - AWS SSO Example](../../common/aws/README.md#example-5-aws-sso-aws-sdk) for SSO setup details.

### Example 5: Connection Pool Configuration

```java
import java.util.Properties;

Properties props = new Properties();
props.setProperty("jclouds.datasource.max-pool-size", "20");
props.setProperty("jclouds.datasource.min-idle", "5");
props.setProperty("jclouds.datasource.connection-timeout", "30000");
props.setProperty("jclouds.datasource.max-lifetime", "1800000");  // 30 minutes
props.setProperty("jclouds.datasource.idle-timeout", "600000");   // 10 minutes

DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    .credentials("dbuser", "password")
    .overrides(props)
    .buildView(DataSourceContext.class);
```

## Setting Up IAM Database Authentication

### 1. Enable IAM Authentication on RDS Instance

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --enable-iam-database-authentication \
    --apply-immediately
```

### 2. Create Database User for IAM Authentication

```sql
-- For MySQL/MariaDB
CREATE USER 'dbuser' IDENTIFIED WITH AWSAuthenticationPlugin AS 'RDS';
GRANT SELECT, INSERT, UPDATE, DELETE ON mydb.* TO 'dbuser'@'%';

-- For PostgreSQL
CREATE USER dbuser;
GRANT rds_iam TO dbuser;
GRANT ALL PRIVILEGES ON DATABASE mydb TO dbuser;
```

### 3. Grant IAM Permission to Connect

Create IAM policy and attach to your IAM user/role:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "rds-db:connect",
            "Resource": "arn:aws:rds-db:us-east-1:123456789012:dbuser:db-ABCDEFGHIJKL/dbuser"
        }
    ]
}
```

Replace:
- `us-east-1` with your region
- `123456789012` with your AWS account ID
- `db-ABCDEFGHIJKL` with your DB instance resource ID (get via `aws rds describe-db-instances`)
- `dbuser` with your database username

### 4. Configure SSL/TLS (Required for IAM Auth)

Download RDS CA certificate:

```bash
wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
```

Add SSL parameters to JDBC URL:

```java
String jdbcUrl = "jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb" +
    "?useSSL=true" +
    "&requireSSL=true" +
    "&serverSslCert=/path/to/global-bundle.pem";

DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint(jdbcUrl)
    .credentials("dbuser", "")
    .buildView(DataSourceContext.class);
```

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `jclouds.datasource.max-pool-size` | Maximum connection pool size | `10` |
| `jclouds.datasource.min-idle` | Minimum idle connections | `5` |
| `jclouds.datasource.connection-timeout` | Connection timeout (ms) | `30000` |
| `jclouds.datasource.max-lifetime` | Max connection lifetime (ms) | `1800000` (30 min) |
| `jclouds.datasource.idle-timeout` | Idle connection timeout (ms) | `600000` (10 min) |

## Maven Dependency

```xml
<dependency>
    <groupId>org.apache.jclouds.provider</groupId>
    <artifactId>aws-rds</artifactId>
    <version>2.8.0</version>
</dependency>

<!-- JDBC driver (choose based on your database) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>

<!-- For IAM authentication, add AWS SDK dependencies -->
<!-- See "Required AWS SDK Dependencies" section above -->
```

## Troubleshooting

### Enable Debug Logging

```java
// Enable jclouds logging
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
    .endpoint(jdbcUrl)
    .credentials("dbuser", "")
    .modules(ImmutableSet.of(new SLF4JLoggingModule()))
    .buildView(DataSourceContext.class);
```

Add to your `logback.xml`:

```xml
<configuration>
    <logger name="org.jclouds.aws.rds" level="DEBUG"/>
    <logger name="org.jclouds.datasource" level="DEBUG"/>
</configuration>
```

### Enable AWS Credential Debug Logging

```bash
java -Djclouds.aws.credentials.debug=true -jar your-app.jar
```

See [AWS Common README - Debugging Credentials](../../common/aws/README.md#debugging-credentials).

### Common Issues

**Issue**: Authentication token length is too short (~300 chars instead of ~1300 chars)
**Solution**: You're using static AWS credentials instead of temporary STS credentials. IAM database authentication requires temporary credentials with session tokens. Use AWS SSO, EC2 instance roles, EKS IRSA, or STS `GetSessionToken` for temporary credentials.

**Issue**: `Access denied for user 'dbuser'@'...'`
**Solution**:
- Verify database user is created with `AWSAuthenticationPlugin` (MySQL) or `rds_iam` role (PostgreSQL)
- Check IAM policy grants `rds-db:connect` permission
- Ensure SSL/TLS is configured correctly

**Issue**: `NoSuchMethodError` or `ClassNotFoundException` for AWS SDK classes
**Solution**: Include all required AWS SDK dependencies. See [Dependencies](#required-aws-sdk-dependencies-for-iam-auth).

**Issue**: Region detection failure
**Solution**: Set region explicitly via system property: `-Daws-rds.region=us-east-1` or environment variable: `export AWS_REGION=us-east-1`

**Issue**: Token expired errors
**Solution**: Tokens auto-refresh, but if AWS credentials themselves expire (e.g., SSO session), run `aws sso login` again.

## Running Integration Tests

```bash
# With static password
mvn test -pl providers/aws-rds \
  -Dtest=AWSRdsDataSourceContextIntegrationTest \
  -Dtest.aws-rds.endpoint=jdbc:mysql://mydb.region.rds.amazonaws.com:3306/testdb \
  -Dtest.aws-rds.identity=admin \
  -Dtest.aws-rds.credential=mypassword

# With IAM authentication (requires AWS SDK dependencies and proper setup)
mvn test -pl providers/aws-rds \
  -Dtest=AWSRdsDataSourceContextIntegrationTest \
  -Dtest.aws-rds.endpoint=jdbc:mysql://mydb.region.rds.amazonaws.com:3306/testdb \
  -Dtest.aws-rds.identity=iamuser \
  -Dtest.aws-rds.credential=
```

## Additional Resources

- [AWS RDS IAM Authentication Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html)
- [AWS Common README](../../common/aws/README.md) - Credential configuration
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)
- [jclouds DataSource Guide](../../datasource/README.md)
