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
package org.jclouds.aws.credentials;

import com.google.common.base.Supplier;

import org.jclouds.domain.Credentials;
import org.jclouds.aws.domain.SessionCredentials;
import org.jclouds.logging.Logger;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * AWS credentials provider that resolves credentials using the AWS SDK v2 default credentials chain.
 * <p>
 * This class is <b>optional</b> and only used when AWS SDK v2 dependencies are available on the classpath.
 * If AWS SDK is not present, users must provide explicit credentials via {@code credentials()} method.
 * <p>
 * When AWS SDK is available, credentials are resolved through the standard credential provider chain:
 * <ol>
 *   <li>Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)</li>
 *   <li>Java system properties (aws.accessKeyId, aws.secretKey)</li>
 *   <li>Web Identity Token from AWS STS (for EKS/IRSA)</li>
 *   <li>Shared credentials file (~/.aws/credentials)</li>
 *   <li>AWS configuration file (~/.aws/config)</li>
 *   <li>Amazon ECS container credentials (for ECS tasks)</li>
 *   <li>Amazon EC2 instance profile credentials (for EC2 instances with IAM roles)</li>
 * </ol>
 * <p>
 * <b>Note on Dependency Injection:</b> This class is typically instantiated in API metadata builders
 * before the Guice injector is created in {@code ContextBuilder.buildInjector()}. Therefore, dependency
 * injection (including {@code @Resource} logger injection) does not work.
 * <p>
 * <b>Debug Logging:</b> By default, logging is disabled. To enable debug logging for credential resolution,
 * set the system property {@code jclouds.aws.credentials.debug=true}:
 * <pre>
 * java -Djclouds.aws.credentials.debug=true -jar your-app.jar
 * </pre>
 * This writes detailed credential resolution information to System.err via {@code Logger.CONSOLE}.
 */
public class AWSCredentialsProvider {

    /**
     * System property to enable debug logging for AWS credential resolution.
     * Set to "true" to enable console logging: -Djclouds.aws.credentials.debug=true
     */
    private static final String DEBUG_PROPERTY = "jclouds.aws.credentials.debug";

    /**
     * Default AWS region to use when AWS SDK is not available or region detection fails.
     */
    private static final String DEFAULT_REGION = "us-east-1";

    /**
     * Cached check for AWS SDK availability. Determined once at class load time.
     */
    private static final boolean AWS_SDK_AVAILABLE = checkAwsSdkAvailable();

    /**
     * Logger for credential operations. Defaults to Logger.NULL (no logging) unless
     * the system property jclouds.aws.credentials.debug=true is set, in which case
     * Logger.CONSOLE is used to write to System.err.
     */
    protected Logger logger = Boolean.getBoolean(DEBUG_PROPERTY) ? Logger.CONSOLE : Logger.NULL;

    // is a AwsCredentialsProvider, but that class is potentially not present
    private Object credentialsProvider;
    private Region region;

    /**
     * Checks if AWS SDK v2 classes are available on the classpath at runtime.
     * This allows the provider to work with or without AWS SDK dependencies.
     *
     * @return true if AWS SDK v2 auth and regions modules are present, false otherwise
     */
    private static boolean checkAwsSdkAvailable() {
        try {
            Class.forName("software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider");
            Class.forName("software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns whether AWS SDK v2 is available on the classpath.
     * This can be used to conditionally instantiate this provider.
     *
     * @return true if AWS SDK v2 is available, false otherwise
     */
    public static boolean isAwsSdkAvailable() {
        return AWS_SDK_AVAILABLE;
    }

    protected AwsCredentials resolveAwsCredentials() {
        if (!AWS_SDK_AVAILABLE) {
            throw new IllegalStateException(
                    "AWS SDK v2 is not available on the classpath. To use ambient credentials (EC2 instance roles, ECS task roles, " +
                    "EKS/IRSA, SSO, etc.), ensure the following dependencies are present:\n" +
                    "  - software.amazon.awssdk:auth\n" +
                    "  - software.amazon.awssdk:regions\n\n" +
                    "Alternatively, provide explicit credentials using the .credentials() method:\n" +
                    "  ContextBuilder.newBuilder(\"aws-s3\")\n" +
                    "    .credentials(\"AKIAIOSFODNN7EXAMPLE\", \"wJalr...\")\n" +
                    "    .buildView(BlobStoreContext.class);");
        }

        try {
            // Cache the provider instance (not the credentials) to avoid recreating it on every call.
            // The AWS SDK's DefaultCredentialsProvider handles credential caching and refresh internally.
            if (this.credentialsProvider == null) {
                this.credentialsProvider = DefaultCredentialsProvider.create();
            }

            logger.info(Logger.formatWithContext("Retrieving AWS credentials..."));
            AwsCredentials awsCredentials = ((AwsCredentialsProvider)this.credentialsProvider).resolveCredentials();
            
            // INFO: High-level success message for operational visibility
            logger.info(Logger.formatWithContext("Successfully retrieved "
                    + (awsCredentials instanceof AwsSessionCredentials ? "temporary" : "permanent")
                    + " AWS credentials from default credentials provider chain:"));

            // DEBUG: Detailed credential information for troubleshooting (contains sensitive data)
            // Note: Should be DEBUG but Logger.CONSOLE doesn't support DEBUG level (isDebugEnabled() returns false)
            logger.info(Logger.formatWithContext("- Access Key ID: " + awsCredentials.accessKeyId()));
            logger.info(Logger.formatWithContext("- Secret Access Key: " + awsCredentials.secretAccessKey().substring(0, Math.min(8, awsCredentials.secretAccessKey().length())) + "..."));
            if (awsCredentials instanceof AwsSessionCredentials) {
                AwsSessionCredentials awsSessionCredentials = (AwsSessionCredentials) awsCredentials;
                String sessionToken = awsSessionCredentials.sessionToken();
                logger.info(Logger.formatWithContext("- Session Token: " + sessionToken.substring(0, Math.min(8, sessionToken.length())) + "..."));
            }
            logger.info(Logger.formatWithContext("- Region: " + getRegion()));
            return awsCredentials;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to retrieve AWS credentials.\n" +
                    "Note: Either use permanent or temporary AWS credentials configured on your local machine by setting the 'AWS_PROFILE' " +
                    "environment variable to the name of the corresponding classic or SSO-enabled AWS CLI profile (make sure you've authenticated beforehand by running " +
                    "`aws sso login` if the second is the case), or run your program or service in an environment that provides ambient AWS credentials, such as:\n" +
                    "- EC2 instance with IAM role\n" +
                    "- ECS task with task role,\n" +
                    "- EKS pod with service account (IRSA)", e);
        }
    }

    public Credentials getCredentials() {
        // Always resolve fresh credentials to allow AWS SDK v2's automatic refresh to work.
        // The AWS SDK's DefaultCredentialsProvider handles caching internally, so there's
        // no performance penalty from calling this on every request.
        AwsCredentials awsCredentials = resolveAwsCredentials();

        if (!(awsCredentials instanceof AwsSessionCredentials)) {
            // Yield permanent credentials
            return new Credentials.Builder<Credentials>()
                    .identity(awsCredentials.accessKeyId())
                    .credential(awsCredentials.secretAccessKey())
                    .build();
        } else {
            // Yield temporary credentials
            AwsSessionCredentials awsSessionCredentials = (AwsSessionCredentials) awsCredentials;
            return SessionCredentials.builder()
                    .accessKeyId(awsSessionCredentials.accessKeyId())
                    .secretAccessKey(awsSessionCredentials.secretAccessKey())
                    .sessionToken(awsSessionCredentials.sessionToken())
                    .expiration(null) // AWS SDK v2 doesn't expose expiration time through credentials
                    .build();
        }
    }

    /**
     * Returns a credentials supplier if AWS SDK is available, or null otherwise.
     * <p>
     * This method is designed to be called during API metadata initialization where AWS SDK
     * may or may not be present. If AWS SDK is not available, returns null to allow the
     * credentials priority system to fall back to explicit credentials or other sources.
     * <p>
     * If AWS SDK is available, returns a supplier that lazily resolves credentials using
     * the AWS SDK default credentials chain.
     *
     * @return credentials supplier, or null if AWS SDK is not available
     */
    public Supplier<Credentials> getCredentialsSupplier() {
        if (!AWS_SDK_AVAILABLE) {
            return null;
        }
        return () -> {
            return getCredentials();
        };
    }

    /**
     * Returns the AWS region to use for API calls.
     * <p>
     * If AWS SDK is available, uses DefaultAwsRegionProviderChain which checks:
     * <ol>
     *   <li>AWS_REGION environment variable</li>
     *   <li>aws.region system property</li>
     *   <li>~/.aws/config file</li>
     *   <li>EC2 instance metadata</li>
     * </ol>
     * <p>
     * If AWS SDK is not available or region detection fails, returns {@value #DEFAULT_REGION}.
     *
     * @return AWS region identifier (e.g., "us-east-1")
     */
    public String getRegion() {
        if (!AWS_SDK_AVAILABLE) {
            logger.info(Logger.formatWithContext("AWS SDK not available, using default region: " + DEFAULT_REGION));
            return DEFAULT_REGION;
        }

        if (this.region == null) {
            try {
                this.region = new DefaultAwsRegionProviderChain().getRegion();
            } catch (Exception e) {
                logger.warn(Logger.formatWithContext("Failed to detect AWS region, using default: " + DEFAULT_REGION));
                return DEFAULT_REGION;
            }
        }

        return this.region.id();
    }
}
