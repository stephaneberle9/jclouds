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
package org.jclouds.aws.s3;

import static org.jclouds.aws.credentials.BlackHoleMetadataService.skipUnlessTheInstanceMetadataPathIsReachable;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.jclouds.aws.credentials.AWSCredentialsProvider;
import org.jclouds.aws.credentials.BlackHoleMetadataService;
import org.testng.annotations.Test;

/**
 * Provider metadata is constructed for every provider on the classpath whenever any context is
 * built, so its construction must never wait for the network. Before the fix the aws-s3 metadata
 * asked the EC2 instance metadata service for a region every time, which away from EC2 cost one
 * connect timeout per attempt, dozens of attempts per application start.
 * <p>
 * The AWS SDK is on this module's test classpath on purpose (test scope in the POM): without it
 * the region falls back to the default before any lookup, and the test would prove nothing. Both
 * tests change JVM-wide system properties, so the class runs in its own surefire execution
 * without parallelism (the {@code *InstanceMetadataTest} pattern in the POM); see
 * {@link BlackHoleMetadataService}.
 */
@Test(groups = "unit", testName = "AWSS3ApiMetadataInstanceMetadataTest", singleThreaded = true)
public class AWSS3ApiMetadataInstanceMetadataTest {

   @Test(timeOut = 30_000)
   public void testMetadataConstructionNeverContactsTheInstanceMetadataService() throws IOException {
      assertTrue(AWSCredentialsProvider.isAwsSdkAvailable(), "the AWS SDK must be on the test classpath");
      skipUnlessTheInstanceMetadataPathIsReachable();
      try (BlackHoleMetadataService imds = new BlackHoleMetadataService()) {
         AWSS3ApiMetadata api = new AWSS3ApiMetadata();
         new AWSS3ProviderMetadata();
         assertEquals(imds.connections(), 0,
               "constructing the aws-s3 metadata must not open a connection to the instance metadata service");
         assertEquals(api.getDefaultProperties().getProperty(PROPERTY_REGION), "us-east-1",
               "with no region configured anywhere the metadata should carry the default region");
      }
   }

   @Test
   public void testMetadataCarriesTheConfiguredRegion() {
      String original = System.getProperty("aws.region");
      try {
         System.setProperty("aws.region", "eu-central-1");
         assertEquals(new AWSS3ApiMetadata().getDefaultProperties().getProperty(PROPERTY_REGION), "eu-central-1");
      } finally {
         if (original != null) {
            System.setProperty("aws.region", original);
         } else {
            System.clearProperty("aws.region");
         }
      }
   }
}
