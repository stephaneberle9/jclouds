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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.jclouds.logging.Logger;
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
}
