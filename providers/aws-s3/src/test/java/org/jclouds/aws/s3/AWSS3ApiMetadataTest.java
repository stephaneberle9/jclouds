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

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Provider metadata is constructed for every provider on the classpath whenever any context is
 * built, so its construction must never wait for the network. Before this test the aws-s3
 * metadata asked the EC2 instance metadata service for a region every time, which away from EC2
 * cost one connect timeout per attempt, dozens of attempts per application start.
 */
@Test(groups = "unit", testName = "AWSS3ApiMetadataTest", singleThreaded = true)
public class AWSS3ApiMetadataTest {

   @Test
   public void testMetadataConstructionDoesNotWaitForInstanceMetadata() {
      String original = System.getProperty("aws.region");
      try {
         System.clearProperty("aws.region");
         long start = System.nanoTime();
         new AWSS3ApiMetadata();
         new AWSS3ProviderMetadata();
         long millis = (System.nanoTime() - start) / 1_000_000L;
         assertTrue(millis < 800, "Constructing the aws-s3 metadata took " + millis
               + " ms; it must not consult the instance metadata service");
      } finally {
         if (original != null) {
            System.setProperty("aws.region", original);
         }
      }
   }

}
