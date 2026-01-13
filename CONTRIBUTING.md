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
# Contributing to Apache jclouds

Thank you for your interest in contributing to Apache jclouds! This document covers development setup, testing, code quality standards, and the release process.

For additional information, please review:
- [How to Contribute](https://cwiki.apache.org/confluence/display/JCLOUDS/Contribute) - Patch submission and review process
- [Coding Standards](https://cwiki.apache.org/confluence/display/JCLOUDS/Writing+jclouds+Code) - Project coding standards

---

## Table of Contents

- [Development Setup](#development-setup)
- [Repository Structure](#repository-structure)
- [Running the Project](#running-the-project)
  - [Building the Project](#building-the-project)
  - [Running Tests](#running-tests)
  - [Working with Specific Modules](#working-with-specific-modules)
- [Code Quality](#code-quality)
  - [Checkstyle](#checkstyle)
  - [License Headers](#license-headers)
  - [Code Formatting](#code-formatting)
- [Testing](#testing)
  - [Test Framework](#test-framework)
  - [Running Tests](#running-tests-1)
  - [Platform-Specific Issues](#platform-specific-issues)
- [CI/CD Workflows](#cicd-workflows)
  - [Automated Testing](#automated-testing)
  - [Deployment Pipelines](#deployment-pipelines)
- [Creating a Release](#creating-a-release)
- [Development Workflow](#development-workflow)

---

## Development Setup

### Prerequisites

- **Java Development Kit (JDK)**: Java 8 or later
- **Maven**: The project includes a Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` on Unix/Linux), so no manual Maven installation is required
- **Git**: For version control
- **IDE** (optional but recommended): IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Initial Setup

1. **Clone the repository:**

```bash
git clone https://github.com/stephaneberle9/jclouds.git
cd jclouds
```

2. **Verify your Java installation:**

```cmd
java -version
```

You should see Java 8 or later.

3. **Build the project** to ensure everything is set up correctly:

```cmd
# Windows
mvnw.cmd -DskipTests package

# Unix/Linux/macOS
./mvnw -DskipTests package
```

The Maven wrapper will automatically download Maven 3.9.9 on first use.

4. **Import into your IDE:**

Most modern IDEs can import Maven projects directly:
- **IntelliJ IDEA**: File → Open → Select the root `pom.xml`
- **Eclipse**: File → Import → Maven → Existing Maven Projects
- **VS Code**: Open the folder and install the "Extension Pack for Java"

---

## Repository Structure

Apache jclouds is a multi-module Maven project with a modular architecture:

```
jclouds/
├── apis/                    # Protocol-level API modules
│   ├── filesystem/         # Filesystem blob storage API
│   ├── s3/                 # S3 protocol implementation
│   ├── ec2/                # EC2 protocol implementation
│   └── ...
├── providers/              # Cloud provider implementations
│   ├── aws-s3/            # AWS S3 provider
│   ├── azureblob/         # Azure Blob Storage provider
│   └── ...
├── blobstore/             # Blob storage abstractions
├── compute/               # Compute service abstractions
├── core/                  # Core jclouds components
├── drivers/               # HTTP and other drivers
│   ├── okhttp/           # OkHttp driver
│   ├── apachehc/         # Apache HttpClient driver
│   └── ...
├── common/                # Shared platform code
│   ├── openstack/        # OpenStack common code
│   ├── googlecloud/      # Google Cloud common code
│   └── ...
├── loadbalancer/         # Load balancer abstractions
├── scriptbuilder/        # Script building utilities
├── resources/            # Build configuration
│   ├── checkstyle.xml
│   └── checkstyle-suppressions.xml
├── project/              # Parent POM configuration
│   └── pom.xml
├── pom.xml               # Root aggregator POM
├── mvnw.cmd              # Maven wrapper for Windows
├── mvnw                  # Maven wrapper for Unix/Linux
└── CLAUDE.md             # AI assistant guidance
```

### Key Architecture Patterns

- **Dependency Injection**: Uses Google Guice extensively for DI and module wiring
- **Platform-Agnostic Design**: Core abstractions + provider implementations
- **OSGi Bundles**: Most modules are OSGi-aware with `bnd.bnd` files

---

## Running the Project

### Building the Project

**Important**: Always use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` on Unix/Linux) to ensure consistent builds.

#### Full Build (Skip Tests)

```cmd
# Windows
mvnw.cmd -DskipTests package

# Unix/Linux/macOS
./mvnw -DskipTests package
```

This compiles all modules and creates JARs in each module's `target/` directory.

#### Full Build with Tests

```cmd
mvnw.cmd package
```

**Warning**: Running all tests can take significant time. Consider running tests for specific modules instead.

#### Build with Documentation

```cmd
mvnw.cmd -DskipTests package -Pdoc,src
```

Profiles:
- `doc`: Generates Javadoc JARs
- `src`: Generates source JARs

These profiles are used during release builds.

### Running Tests

#### Run All Tests for a Module

```cmd
# Windows
mvnw.cmd -pl apis/filesystem -am test

# Unix/Linux/macOS
./mvnw -pl apis/filesystem -am test
```

Flags:
- `-pl`: Project list - specifies which module(s) to build
- `-am`: Also make - builds dependencies as well

#### Run a Specific Test Class

```cmd
mvnw.cmd -pl apis/filesystem -am -Dtest=FilesystemBlobStoreTest test
```

#### Run a Single Test Method

```cmd
mvnw.cmd -pl apis/filesystem -am -Dtest=FilesystemBlobStoreTest#testMethodName test
```

Replace `#` with the literal hash character.

#### Test Reports

Test reports are generated in:
```
<module>/target/surefire-reports/
```

HTML reports:
```
<module>/target/surefire-reports/Surefire suite/<TestClassName>.html
```

### Working with Specific Modules

#### Example: Filesystem API

```cmd
# Build just the filesystem module and its dependencies
mvnw.cmd -pl apis/filesystem -am package

# Run all filesystem tests
mvnw.cmd -pl apis/filesystem -am test

# Run a specific test
mvnw.cmd -pl apis/filesystem -am -Dtest=FilesystemStorageStrategyImplTest test
```

#### Example: Multiple Modules

```cmd
# Build multiple modules
mvnw.cmd -pl apis/filesystem,apis/s3 -am package

# Test multiple modules
mvnw.cmd -pl apis/filesystem,apis/s3 -am test
```

---

## Code Quality

### Checkstyle

The project enforces code style using Checkstyle during the `verify` phase.

#### Configuration Files

- **Rules**: `resources/checkstyle.xml`
- **Suppressions**: `resources/checkstyle-suppressions.xml`

#### Running Checkstyle

```cmd
# Verify entire project
mvnw.cmd verify

# Verify specific module
mvnw.cmd -pl apis/filesystem -am verify

# Run checkstyle without tests
mvnw.cmd verify -DskipTests
```

Checkstyle violations will fail the build. Address all violations before committing.

### License Headers

All source files must include the Apache License header. The Apache RAT (Release Audit Tool) plugin checks for license headers during the `package` phase.

#### Required License Header (Java)

```java
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
```

For XML/HTML files, use `<!-- ... -->` comment syntax.

#### Running RAT Checks

```cmd
# RAT runs automatically during package
mvnw.cmd package

# Run RAT explicitly
mvnw.cmd apache-rat:check
```

RAT reports are generated in `target/rat.txt` for modules that fail the check.

#### Files Excluded from RAT

Configured in `project/pom.xml`:
- Test resources (`.txt`, `.xml`, `.sh` in `src/test/resources/`)
- META-INF/services files
- OSGi metadata (`bnd.bnd`)
- Build/IDE artifacts (`target/`, `.idea/`, `*.iml`)
- Git metadata (`.gitignore`, `.gitattributes`)
- Documentation files (`README.md`, `NOTICE.txt`)

### Code Formatting

Follow the [jclouds Coding Standards](https://cwiki.apache.org/confluence/display/JCLOUDS/Writing+jclouds+Code):

- **Indentation**: 3 spaces (yes, 3!)
- **Line length**: 120 characters maximum
- **Imports**: No wildcards, organize imports
- **Naming**: Follow Java conventions (CamelCase for classes, camelCase for methods/variables)
- **Comments**: Javadoc for public APIs, inline comments for complex logic

---

## Testing

### Test Framework

jclouds uses **TestNG** for testing:

```java
import org.testng.annotations.Test;

public class MyTest {
   @Test
   public void testSomething() {
      // Test code
   }
}
```

### Running Tests

See [Running Tests](#running-tests-1) section above.

### Test Best Practices

1. **Use descriptive test names**: `testCreateBlobWithMetadata()` not `test1()`
2. **Test one thing per test**: Keep tests focused and atomic
3. **Use assertions**: Leverage TestNG assertions (`assertEquals`, `assertTrue`, etc.)
4. **Clean up resources**: Use `@AfterMethod` or `@AfterClass` for cleanup
5. **Avoid test interdependencies**: Tests should be independent and runnable in any order

### Platform-Specific Issues

#### Windows File Locking

The filesystem provider tests are sensitive to file locks on Windows. Unclosed streams prevent file deletion.

**Critical**: Always close streams using try-with-resources:

```java
// Good
try (InputStream in = blob.getPayload().openStream()) {
   // Use stream
}

// Bad
InputStream in = blob.getPayload().openStream();
// Use stream
in.close(); // May not execute if exception occurs
```

Unclosed `InputStream` or `BinaryChannel` objects will cause test failures on Windows.

---

## CI/CD Workflows

This project uses **GitHub Actions** for continuous integration and deployment. All workflows are located in `.github/workflows/`.

### Automated Testing

**Workflow**: `maven.yml` (or similar)

Runs automatically on:
- Push to `main` branch
- All pull requests
- Manual trigger via workflow dispatch

Actions performed:
- Sets up Java environment
- Runs Maven build with tests: `mvnw.cmd verify`
- Runs Checkstyle and RAT checks
- Reports test results and coverage

### Deployment Pipelines

This fork is configured to deploy artifacts to **[itemis Nexus](https://artifacts.itemis.cloud)**.

#### Snapshot Deployment

**Workflow**: `deploy-snapshot.yml`

Runs automatically on:
- Every push to `main` branch

Actions performed:
- Builds the project: `mvnw.cmd -DskipTests package`
- Deploys snapshot artifacts to itemis Nexus
- Uses credentials from GitHub secrets: `itemis_NEXUS_USER` and `itemis_NEXUS_PASS`

Snapshot version format: `2.9.0-SNAPSHOT`

#### Release Deployment

**Workflow**: `deploy-release.yml`

Runs automatically when:
- A tag with `v` prefix is pushed (e.g., `v2.9.0`)

Actions performed:
- Validates that version is not a snapshot
- Builds the project with documentation: `mvnw.cmd -DskipTests package -Pdoc,src`
- Deploys release artifacts to itemis Nexus
- Uses credentials from GitHub secrets

Release version format: `2.9.0` (no `-SNAPSHOT` suffix)

#### Local Deployment Testing

For local testing of the deployment process:

```cmd
# Set credentials as environment variables (Windows)
set NEXUS_USER=your-username
set NEXUS_PASS=your-password

# Deploy using provided settings.xml
mvnw.cmd -DskipTests deploy -s settings.xml
```

The `settings.xml` file reads credentials from `NEXUS_USER` and `NEXUS_PASS` environment variables.

---

## Creating a Release

To create a new release and deploy to itemis Nexus:

### 1. Prepare the Release

Ensure the `main` branch is in a releasable state:

```cmd
# Run full build with tests and quality checks
mvnw.cmd verify

# Ensure all tests pass
mvnw.cmd test

# Check license headers
mvnw.cmd apache-rat:check
```

### 2. Update Version

Update the version in the root `pom.xml` and all module POMs:

```xml
<version>2.9.0</version>  <!-- Remove -SNAPSHOT suffix -->
```

Commit the version change:

```bash
git add .
git commit -m "release: Prepare version 2.9.0"
git push origin main
```

### 3. Create and Push a Version Tag

```bash
# Create annotated tag
git tag -a v2.9.0 -m "Release version 2.9.0"

# Push tag to trigger release deployment
git push origin v2.9.0
```

### 4. Automatic Deployment

The `deploy-release.yml` workflow will automatically:
- Validate that the version is not a snapshot
- Build the project with tests and documentation
- Deploy artifacts to itemis Nexus release repository

### 5. Prepare for Next Development Cycle

Update the version to the next snapshot:

```xml
<version>2.10.0-SNAPSHOT</version>
```

Commit and push:

```bash
git add .
git commit -m "chore: bump version to 2.10.0-SNAPSHOT"
git push origin main
```

### Version Format

- **Snapshot versions**: `X.Y.Z-SNAPSHOT` (e.g., `2.9.0-SNAPSHOT`)
- **Release versions**: `X.Y.Z` (e.g., `2.9.0`)

The CI pipeline prevents deploying snapshot versions to the release repository.

---

## Development Workflow

### 1. Small, Focused Changes

Edit one module and run its tests:

```cmd
# Make code changes in apis/filesystem
mvnw.cmd -pl apis/filesystem -am test
```

### 2. Larger Changes

Build everything without tests first, then run focused test suites:

```cmd
# Fast build to check compilation
mvnw.cmd -DskipTests package

# Run tests for affected modules
mvnw.cmd -pl apis/filesystem,apis/s3 -am test
```

### 3. Before Committing

Always run these checks:

```cmd
# 1. Run all tests for affected modules
mvnw.cmd -pl <your-module> -am test

# 2. Verify checkstyle
mvnw.cmd -pl <your-module> -am verify

# 3. Check license headers
mvnw.cmd -pl <your-module> -am package

# Or run everything at once
mvnw.cmd -pl <your-module> -am verify
```

### 4. Creating a Pull Request

1. **Fork the repository** on GitHub
2. **Create a feature branch**:
   ```bash
   git checkout -b feature/my-feature
   ```
3. **Make your changes** following the coding standards
4. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: Add new feature description"
   ```
5. **Push to your fork**:
   ```bash
   git push origin feature/my-feature
   ```
6. **Open a Pull Request** targeting the `main` branch
7. **Address review feedback** and update your PR as needed

### Commit Message Format

Use conventional commit format:

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

Examples:
```
feat(blobstore): Add support for multipart uploads
fix(filesystem): Close streams to prevent Windows file lock issues
docs(contributing): Update release process documentation
```

### Git Workflow

- **Main development branch**: `main`
- **Target for PRs**: `main`
- **Fork**: `github.com/stephaneberle9/jclouds`

---

## Key Dependencies

Understanding these dependencies helps when developing:

- **Google Guice**: Dependency injection framework
- **Google Guava**: Utility libraries
- **GSON**: JSON processing
- **JAX-RS (Jakarta)**: REST API framework
- **TestNG**: Testing framework
- **OSGi**: Modular packaging

Refer to module `pom.xml` files for specific versions.

---

## Getting Help

- **Documentation**: [Apache jclouds Wiki](https://cwiki.apache.org/confluence/display/JCLOUDS)
- **Mailing Lists**: [jclouds mailing lists](https://jclouds.apache.org/community/)
- **Issue Tracker**: [Apache jclouds JIRA](https://issues.apache.org/jira/browse/JCLOUDS)
- **Stack Overflow**: Tag your questions with `jclouds`

---

## License

Apache jclouds is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

All contributions must be made under this license.

---

Thank you for contributing to Apache jclouds!

The Apache jclouds team
