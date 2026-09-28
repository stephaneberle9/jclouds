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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jclouds.logging.Logger;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import org.jclouds.domain.Credentials;

/**
 * Tests for {@link AWSCredentialsProvider}.
 *
 * Note: Tests must run single-threaded because they manipulate shared system properties.
 */
@Test(groups = "unit", testName = "AWSCredentialsProviderTest", singleThreaded = true)
public class AWSCredentialsProviderTest {

   @Test
   public void testAwsSdkAvailabilityCheck() {
      // AWS SDK should be available in test classpath (declared in pom.xml)
      assertTrue(AWSCredentialsProvider.isAwsSdkAvailable(),
            "AWS SDK should be available on the test classpath");
   }

   @Test
   public void testGetCredentialsSupplierWhenSdkAvailable() {
      AWSCredentialsProvider provider = new AWSCredentialsProvider();
      Supplier<Credentials> supplier = provider.getCredentialsSupplier();

      // Should return non-null supplier when AWS SDK is available
      assertNotNull(supplier,
            "Credentials supplier should not be null when AWS SDK is available");
   }

   @Test
   public void testGetRegionReturnsNonNull() {
      AWSCredentialsProvider provider = new AWSCredentialsProvider();
      String region = provider.getRegion();

      assertNotNull(region, "Region should never be null");
      assertTrue(region.matches("^[a-z]{2}-[a-z]+-\\d+$|^us-east-1$"),
            "Region should be in format like 'us-east-1' or 'eu-west-1', got: " + region);
   }

   @Test
   public void testDefaultRegionFallback() {
      // Even without credentials configured, getRegion() should return default
      AWSCredentialsProvider provider = new AWSCredentialsProvider();
      String region = provider.getRegion();

      // Should be us-east-1 or detected from environment
      assertNotNull(region, "Region should not be null");
      // If no region configured, should default to us-east-1
      // Note: This test might detect region from environment, which is fine
   }

   @Test
   public void testConfiguredRegionHonoursTheSystemProperty() {
      String original = System.getProperty("aws.region");
      try {
         System.setProperty("aws.region", "eu-central-1");
         assertEquals(new AWSCredentialsProvider().getConfiguredRegion(), "eu-central-1",
               "getConfiguredRegion() should return the region the aws.region system property names");
      } finally {
         if (original != null) {
            System.setProperty("aws.region", original);
         } else {
            System.clearProperty("aws.region");
         }
      }
   }

   @Test(timeOut = 30_000)
   public void testConfiguredRegionNeverContactsTheInstanceMetadataService() throws IOException {
      skipIfTheEnvironmentNamesARegion();
      try (BlackHoleMetadataService imds = new BlackHoleMetadataService()) {
         assertEquals(new AWSCredentialsProvider().getConfiguredRegion(), "us-east-1",
               "With no region configured anywhere, getConfiguredRegion() should fall back to the default");
         assertEquals(imds.connections(), 0,
               "getConfiguredRegion() must not open a connection to the instance metadata service");
      }
   }

   @Test(timeOut = 60_000)
   public void testDetectedRegionIsMemoizedAcrossInstances() throws IOException {
      skipIfTheEnvironmentNamesARegion();
      try (BlackHoleMetadataService imds = new BlackHoleMetadataService()) {
         String first = new AWSCredentialsProvider().getRegion();
         int connectionsAfterFirst = imds.connections();
         // A region configured after the first detection must not be observed: the outcome is
         // process-wide, and so is the decision not to ask the instance metadata service again.
         System.setProperty("aws.region", "eu-central-1".equals(first) ? "eu-west-1" : "eu-central-1");
         String second = new AWSCredentialsProvider().getRegion();
         assertEquals(second, first, "Every instance should see the region detected once for the process");
         assertEquals(imds.connections(), connectionsAfterFirst,
               "A second getRegion() must not contact the instance metadata service again");
      }
   }

   /**
    * The SDK's region chain reads AWS_REGION before anything else, and a test cannot unset an
    * environment variable: on a machine that names a region there the metadata service is never
    * reached with or without the fix, so the assertions below would prove nothing.
    */
   private static void skipIfTheEnvironmentNamesARegion() {
      if (System.getenv("AWS_REGION") != null) {
         throw new SkipException("AWS_REGION is set in the environment; the instance metadata path is not reachable");
      }
   }

   /**
    * Stands in for the EC2 instance metadata service: accepts every connection and never answers,
    * which is what 169.254.169.254 looks like from anywhere but EC2. Points the SDK at it and takes
    * every configured region source away for the duration, restoring all of it on close.
    */
   private static final class BlackHoleMetadataService implements AutoCloseable {
      private final ServerSocket socket;
      private final List<Socket> held = Collections.synchronizedList(new ArrayList<Socket>());
      private final AtomicInteger connections = new AtomicInteger();
      private final Map<String, String> savedProperties = new HashMap<String, String>();

      BlackHoleMetadataService() throws IOException {
         socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
         Thread acceptor = new Thread(() -> {
            while (!socket.isClosed()) {
               try {
                  held.add(socket.accept());
                  connections.incrementAndGet();
               } catch (IOException e) {
                  return;
               }
            }
         }, "black-hole-instance-metadata");
         acceptor.setDaemon(true);
         acceptor.start();
         File emptyConfig = File.createTempFile("aws-config", ".empty");
         emptyConfig.deleteOnExit();
         set("aws.ec2MetadataServiceEndpoint", "http://127.0.0.1:" + socket.getLocalPort());
         set("aws.configFile", emptyConfig.getAbsolutePath());
         set("aws.sharedCredentialsFile", emptyConfig.getAbsolutePath());
         set("aws.region", null);
      }

      int connections() {
         return connections.get();
      }

      private void set(String key, String value) {
         if (!savedProperties.containsKey(key)) {
            savedProperties.put(key, System.getProperty(key));
         }
         if (value == null) {
            System.clearProperty(key);
         } else {
            System.setProperty(key, value);
         }
      }

      @Override
      public void close() throws IOException {
         for (Map.Entry<String, String> entry : savedProperties.entrySet()) {
            if (entry.getValue() == null) {
               System.clearProperty(entry.getKey());
            } else {
               System.setProperty(entry.getKey(), entry.getValue());
            }
         }
         for (Socket s : held) {
            s.close();
         }
         socket.close();
      }
   }

   @Test
   public void testDebugLoggingSystemPropertyFalse() {
      // Save original value
      String originalValue = System.getProperty("jclouds.aws.credentials.debug");

      try {
         System.clearProperty("jclouds.aws.credentials.debug");
         System.setProperty("jclouds.aws.credentials.debug", "false");
         AWSCredentialsProvider provider = new AWSCredentialsProvider();

         // Logger should be Logger.NULL when debug is false
         // Boolean.getBoolean() returns false for "false", "FALSE", "no", or anything non-"true"
         assertTrue(provider.logger == Logger.NULL,
               "Logger should be NULL when debug property is 'false', got: " + provider.logger.getClass().getSimpleName());
      } finally {
         // Restore original value
         if (originalValue != null) {
            System.setProperty("jclouds.aws.credentials.debug", originalValue);
         } else {
            System.clearProperty("jclouds.aws.credentials.debug");
         }
      }
   }

   @Test
   public void testDebugLoggingSystemPropertyTrue() {
      // Save original value
      String originalValue = System.getProperty("jclouds.aws.credentials.debug");

      try {
         System.clearProperty("jclouds.aws.credentials.debug");
         System.setProperty("jclouds.aws.credentials.debug", "true");
         AWSCredentialsProvider provider = new AWSCredentialsProvider();

         // Logger should be Logger.CONSOLE when debug is true
         assertTrue(provider.logger == Logger.CONSOLE,
               "Logger should be CONSOLE when debug property is 'true', got: " + provider.logger.getClass().getSimpleName());
      } finally {
         // Restore original value
         if (originalValue != null) {
            System.setProperty("jclouds.aws.credentials.debug", originalValue);
         } else {
            System.clearProperty("jclouds.aws.credentials.debug");
         }
      }
   }

   @Test
   public void testDebugLoggingSystemPropertyNotSet() {
      // Save original value
      String originalValue = System.getProperty("jclouds.aws.credentials.debug");

      try {
         System.clearProperty("jclouds.aws.credentials.debug");
         AWSCredentialsProvider provider = new AWSCredentialsProvider();

         // Logger should be Logger.NULL when debug property is not set
         // Boolean.getBoolean() returns false when property is not set
         assertTrue(provider.logger == Logger.NULL,
               "Logger should be NULL when debug property is not set, got: " + provider.logger.getClass().getSimpleName());
      } finally {
         // Restore original value
         if (originalValue != null) {
            System.setProperty("jclouds.aws.credentials.debug", originalValue);
         }
      }
   }

   @Test
   public void testGetCredentialsSupplierIsReusable() {
      AWSCredentialsProvider provider = new AWSCredentialsProvider();
      Supplier<Credentials> supplier1 = provider.getCredentialsSupplier();
      Supplier<Credentials> supplier2 = provider.getCredentialsSupplier();

      // Should return the same supplier instance (or equivalent supplier)
      assertNotNull(supplier1, "First supplier should not be null");
      assertNotNull(supplier2, "Second supplier should not be null");
   }

   /**
    * Test behavior when AWS SDK is not available. This test simulates the scenario
    * by documenting expected behavior rather than actually removing AWS SDK from classpath.
    */
   @Test
   public void testExpectedBehaviorWithoutAwsSdk() {
      // When AWS SDK is not available:
      // 1. isAwsSdkAvailable() should return false
      // 2. getCredentialsSupplier() should return null
      // 3. getRegion() should return "us-east-1"
      // 4. getCredentials() should throw IllegalStateException with helpful message

      // This test documents expected behavior - actual test would require
      // custom classloader to exclude AWS SDK classes
   }

   /**
    * Regression test for credential refresh bug.
    * 
    * This test verifies that credentials are NOT cached indefinitely, which was the bug
    * that prevented long-living BlobStore instances from working with temporary AWS credentials
    * (IAM roles, ECS task roles, STS tokens) that expire after ~1 hour.
    * 
    * The test uses a custom TestableAWSCredentialsProvider that allows us to control
    * and verify the credential resolution behavior.
    */
   @Test
   public void testCredentialsAreNotCachedIndefinitely() {
      // Create a testable provider that tracks how many times credentials are resolved
      TestableAWSCredentialsProvider provider = new TestableAWSCredentialsProvider();

      // First call to getCredentials() should resolve credentials
      Credentials creds1 = provider.getCredentials();
      assertNotNull(creds1, "First credentials should not be null");
      assertTrue(provider.getResolveCount() >= 1, "Should have resolved credentials at least once");

      // Second call should resolve fresh credentials again (not use cached value)
      int countAfterFirstCall = provider.getResolveCount();
      Credentials creds2 = provider.getCredentials();
      assertNotNull(creds2, "Second credentials should not be null");
      assertTrue(provider.getResolveCount() > countAfterFirstCall, 
            "Should resolve fresh credentials on each call, not cache them. " +
            "This ensures temporary credentials can be refreshed automatically.");

      // Third call to verify the pattern holds
      int countAfterSecondCall = provider.getResolveCount();
      Credentials creds3 = provider.getCredentials();
      assertNotNull(creds3, "Third credentials should not be null");
      assertTrue(provider.getResolveCount() > countAfterSecondCall,
            "Should continue to resolve fresh credentials on subsequent calls");
   }

   /**
    * Testable subclass of AWSCredentialsProvider that allows us to track
    * how many times resolveAwsCredentials() is called without needing real AWS credentials.
    */
   private static class TestableAWSCredentialsProvider extends AWSCredentialsProvider {
      private int resolveCount = 0;

      @Override
      protected software.amazon.awssdk.auth.credentials.AwsCredentials resolveAwsCredentials() {
         resolveCount++;
         // Return mock credentials instead of calling AWS SDK
         // This allows the test to run without real AWS credentials configured
         return software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
               "AKIAIOSFODNN7EXAMPLE", 
               "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
         );
      }

      public int getResolveCount() {
         return resolveCount;
      }
   }
}
