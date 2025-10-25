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
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apache jclouds is a multi-cloud toolkit for the Java platform providing portable abstractions across cloud providers. This is a large multi-module Maven project with OSGi-friendly bundles.

## Repository Structure

- `apis/` - Protocol-level API modules (e.g., `apis/filesystem/`, `apis/s3/`, `apis/ec2/`)
- `providers/` - Cloud provider implementations (e.g., AWS, Azure, Google Cloud)
- `blobstore/` - Blob storage abstractions
- `compute/` - Compute service abstractions
- `core/` - Core jclouds components and abstractions
- `drivers/` - HTTP and other driver implementations (e.g., `okhttp/`, `apachehc/`)
- `common/` - Shared platform code (e.g., `openstack/`, `googlecloud/`)
- `loadbalancer/` - Load balancer abstractions
- `scriptbuilder/` - Script building utilities
- `resources/` - Build configuration (`checkstyle.xml`, `checkstyle-suppressions.xml`)
- `project/` - Parent POM configuration

The root `pom.xml` is the aggregator that builds all modules.

## Build Commands

**Important**: This repository uses a Maven wrapper to ensure consistent builds without requiring manual Maven installation. Always use:
- **Windows**: `mvnw.cmd`
- **Unix/Linux/WSL**: `./mvnw`

The wrapper automatically downloads and caches Maven 3.9.9 on first use.

### Full Build
```cmd
mvnw.cmd -DskipTests package
```

### Run All Tests for a Module
```cmd
mvnw.cmd -pl apis/filesystem -am test
```

The `-am` (also-make) flag builds dependencies as well, which is typically needed due to inter-module dependencies.

### Run a Specific Test Class
```cmd
mvnw.cmd -pl apis/filesystem -am -Dtest=FilesystemBlobStoreTest test
```

### Run a Single Test Method
```cmd
mvnw.cmd -pl apis/filesystem -am -Dtest=FilesystemBlobStoreTest#methodName test
```

Replace `#` with the literal hash character when running.

### Verify with Checkstyle
```cmd
mvnw.cmd verify
```

Checkstyle rules are enforced via `resources/checkstyle.xml` with suppressions in `resources/checkstyle-suppressions.xml`.

### Build with Documentation and Source JARs
```cmd
mvnw.cmd -DskipTests package -Pdoc,src
```

The `doc` profile generates Javadoc JARs and the `src` profile generates source JARs. These profiles are used during release builds.

## Architecture Patterns

### Dependency Injection
The project uses **Google Guice** extensively for dependency injection and module wiring. Look for `com.google.inject` usages and provider modules when tracing component instantiation.

### Platform-Agnostic Design
jclouds follows a **platform-agnostic implementation + provider pattern**:
- Core abstractions live in `core/` and service-specific modules (`blobstore/`, `compute/`)
- Protocol implementations live in `apis/` (e.g., S3 protocol, filesystem)
- Cloud-specific implementations live in `providers/` (e.g., AWS-S3, Azure Blob)

### OSGi Bundles
Most modules have `bnd.bnd` files and are OSGi-aware. Avoid breaking OSGi metadata unless intentionally changing the packaging model.

## Testing

- Tests use **TestNG** and **Maven Surefire**
- Test reports: `<module>/target/surefire-reports/`
- Example: `apis/filesystem/target/surefire-reports/Surefire suite/FilesystemBlobStoreTest.html`

### Windows-Specific Test Issues
The filesystem provider tests operate on `target/basedir/...` and are **sensitive to file locks on Windows**. Common failures involve unclosed streams preventing file deletion.

**Critical**: When modifying filesystem code, ensure all streams are closed using try-with-resources. Unclosed `InputStream`/`BinaryChannel` objects will cause delete/clear operations to fail on Windows.

## Key Implementation Examples

- **Filesystem blobstore**: `apis/filesystem/src/main/java/org/jclouds/filesystem/strategy/internal/FilesystemStorageStrategyImpl.java`
- **Local blobstore**: Look for `LocalBlobStore` implementations in `apis/filesystem/`

## Development Workflow

1. **Small, focused changes**: Edit one module and run its tests:
   ```cmd
   mvnw.cmd -pl apis/filesystem -am -Dtest=... test
   ```

2. **Larger changes**: Build everything without tests first, then run focused test suites:
   ```cmd
   mvnw.cmd -DskipTests package
   mvnw.cmd -pl apis/filesystem -am test
   ```

3. **Before committing**:
   - Verify checkstyle: `mvnw.cmd verify`
   - Check license headers (runs automatically during package): `mvnw.cmd package`
   - Ensure all tests pass

## License Headers and RAT Checking

All source files (.java, .xml, .sh, .yaml, etc.) **must** include the Apache License header. The Apache RAT (Release Audit Tool) plugin automatically checks for license headers during the `package` phase.

### Required License Header

Java files:
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

XML/YAML files use `<!-- ... -->` comments. The canonical header text is in `project/src/etc/header.txt`.

### Running RAT Checks

RAT runs automatically during package phase:
```cmd
mvnw.cmd package
```

To run RAT explicitly:
```cmd
mvnw.cmd apache-rat:check
```

RAT reports are generated in `target/rat.txt` for each module that fails the check.

### Files Excluded from RAT

The following are excluded from license header requirements (configured in `project/pom.xml`):
- Test resources (`.txt`, `.xml`, `.sh` files in `src/test/resources/`)
- META-INF/services files
- OSGi metadata (`bnd.bnd`)
- Build/IDE artifacts (`target/`, `.idea/`, `*.iml`, etc.)
- Git metadata (`.gitignore`, `.gitattributes`)
- Documentation files (`README.md`, `NOTICE.txt`, etc.)

## Deployment

This fork is configured to deploy artifacts to Itemis Nexus. The deployment configuration is in `project/pom.xml` under `distributionManagement`.

### Local Deployment Testing

A `settings.xml` file is provided for local deployment testing:

```cmd
set NEXUS_USER=your-username
set NEXUS_PASS=your-password
mvnw.cmd -DskipTests deploy -s settings.xml
```

The settings.xml reads credentials from `NEXUS_USER` and `NEXUS_PASS` environment variables.

### CI Deployment

GitHub Actions automatically deploys:
- **Snapshots**: On every push to `main` branch
- **Releases**: When tags with `v` prefix are pushed (e.g., `v2.7.1`)

Deployment credentials are configured via GitHub secrets `ITEMIS_NEXUS_USER` and `ITEMIS_NEXUS_PASS`.

## Git Workflow

- **Main development branch**: `main`
- When creating pull requests, target the `main` branch
- This is a fork of Apache jclouds maintained at `github.com/stephaneberle9/jclouds`

## Key Dependencies

- **Guice** for dependency injection
- **Guava** for utilities
- **GSON** for JSON processing
- **JAX-RS** (Jakarta) for REST APIs
- **TestNG** for testing
- **OSGi** for modular packaging
