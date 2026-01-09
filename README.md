jclouds
======

Apache jclouds is an open source multi-cloud toolkit for the Java platform that gives you the freedom to create applications that are portable across clouds while giving you full control to use cloud-specific features.

For more information about using or contributing to jclouds, please visit our website at [jclouds.apache.org](http://jclouds.apache.org/).

## About This Fork

This is a continuation of the archived upstream Apache jclouds project, maintained with enhancements and modernizations for enterprise use.

### Key Enhancements

#### AWS Ambient Credentials Support
- **AWS SDK Integration**: Native support for AWS SDK credential resolution across AWS providers
- **IAM Roles & SSO**: Seamless authentication using EC2 instance roles, EKS IRSA, AWS SSO, and other ambient credential sources
- **AWS S3 Provider**: Enhanced with ambient credential support, eliminating the need for hardcoded access keys
- **Foundation for Future AWS Providers**: Shared AWS infrastructure ready for other AWS service providers

See [common/aws/README.md](common/aws/README.md) for detailed credential configuration.

#### DataSource Abstraction & Database Providers
- **Generic DataSource API**: Standard JDBC DataSource abstraction for database connectivity
- **AWS RDS Provider**: New provider with HikariCP connection pooling supporting both static password and passwordless IAM authentication
- **Azure Databases Provider**: New provider for Azure PostgreSQL/MySQL with Entra ID authentication and Azure Workload Identity support
- **On-Demand Token Generation**: For passwordless mode, fresh auth tokens generated for each connection using ambient credentials (AWS IAM or Azure Entra ID)

See [providers/aws-rds/README.md](providers/aws-rds/README.md) and [providers/azuredatabases/README.md](providers/azuredatabases/README.md) for usage examples and setup instructions.

#### Build System Improvements
- **CI-Friendly Maven**: Single-source version management using `${revision}` property
- **Optimized CI/CD Pipeline**: GitHub Actions workflow with artifact caching for faster deployments
- **Maven Wrapper**: Consistent builds without requiring manual Maven installation

#### Artifact Publishing
- **itemis Nexus Repository**: Automated deployment to [itemis Nexus Repository Manager](https://artifacts.itemis.cloud)
- **Snapshot Releases**: Automatic snapshot deployments on every main branch commit
- **Tagged Releases**: Version-tagged releases deployed automatically via GitHub Actions

### Using This Fork

Add the itemis Nexus repository to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>itemis-nexus</id>
    <url>https://https://artifacts.itemis.cloud/repository/maven</url>
  </repository>
</repositories>

<dependency>
  <groupId>org.apache.jclouds.provider</groupId>
  <artifactId>aws-s3</artifactId>
  <version>2.8.0</version>
</dependency>

<dependency>
  <groupId>org.apache.jclouds.provider</groupId>
  <artifactId>aws-rds</artifactId>
  <version>2.8.0</version>
</dependency>

<dependency>
  <groupId>org.apache.jclouds.provider</groupId>
  <artifactId>azuredatabases</artifactId>
  <version>2.8.0</version>
</dependency>
```

### Building

This project uses the Maven wrapper for consistent builds:

```bash
# Windows
mvnw.cmd clean install

# Unix/Linux/Mac
./mvnw clean install
```

See [CLAUDE.md](CLAUDE.md) for detailed development workflow and build instructions.

License
-------
Copyright (C) 2009-2025 The Apache Software Foundation and itemis

Licensed under the Apache License, Version 2.0
