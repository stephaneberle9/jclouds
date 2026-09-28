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

import static org.jclouds.aws.credentials.BlackHoleMetadataService.skipUnlessTheInstanceMetadataPathIsReachable;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.testng.annotations.Test;

/**
 * Region resolution against a stand-in instance metadata service that never answers. Every test
 * here changes JVM-wide system properties, so the class runs in its own surefire execution
 * without parallelism (the {@code *InstanceMetadataTest} pattern in the POM); see
 * {@link BlackHoleMetadataService}.
 */
@SuppressWarnings("deprecation") // getRegion() stays public API; its memoization is what is under test
@Test(groups = "unit", testName = "AWSCredentialsProviderInstanceMetadataTest", singleThreaded = true)
public class AWSCredentialsProviderInstanceMetadataTest {

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
      skipUnlessTheInstanceMetadataPathIsReachable();
      try (BlackHoleMetadataService imds = new BlackHoleMetadataService()) {
         assertEquals(new AWSCredentialsProvider().getConfiguredRegion(), "us-east-1",
               "With no region configured anywhere, getConfiguredRegion() should fall back to the default");
         assertEquals(imds.connections(), 0,
               "getConfiguredRegion() must not open a connection to the instance metadata service");
      }
   }

   /**
    * Drives one real detection against the black hole and proves the outcome is remembered:
    * without the reset, a detection run by an earlier test would already be cached and the
    * second half of this test would compare zero connections with zero.
    */
   @Test(timeOut = 60_000)
   public void testFailedDetectionIsMemoizedAcrossInstances() throws IOException {
      skipUnlessTheInstanceMetadataPathIsReachable();
      AWSCredentialsProvider.resetDetectedRegionForTesting();
      try (BlackHoleMetadataService imds = new BlackHoleMetadataService()) {
         String first = new AWSCredentialsProvider().getRegion();
         int connectionsAfterFirst = imds.connections();
         assertTrue(connectionsAfterFirst > 0,
               "the first getRegion() should have run the SDK chain into the instance metadata service");
         assertEquals(first, "us-east-1", "a detection that fails should fall back to the default region");
         // A region configured after the detection must not be observed: the outcome is
         // process-wide, and so is the decision not to ask the instance metadata service again.
         System.setProperty("aws.region", "eu-central-1");
         String second = new AWSCredentialsProvider().getRegion();
         assertEquals(second, first, "every instance should see the region detected once for the process");
         assertEquals(imds.connections(), connectionsAfterFirst,
               "a second getRegion() must not contact the instance metadata service again");
      } finally {
         AWSCredentialsProvider.resetDetectedRegionForTesting();
      }
   }
}
