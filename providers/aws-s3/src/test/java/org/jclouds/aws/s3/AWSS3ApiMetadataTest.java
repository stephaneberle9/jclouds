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

import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGION;
import static org.testng.Assert.assertEquals;
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

import org.jclouds.aws.credentials.AWSCredentialsProvider;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Provider metadata is constructed for every provider on the classpath whenever any context is
 * built, so its construction must never wait for the network. Before the fix the aws-s3 metadata
 * asked the EC2 instance metadata service for a region every time, which away from EC2 cost one
 * connect timeout per attempt, dozens of attempts per application start.
 * <p>
 * The AWS SDK is on this module's test classpath on purpose (test scope in the pom): without it
 * the region falls back to the default before any lookup, and the test would prove nothing.
 */
@Test(groups = "unit", testName = "AWSS3ApiMetadataTest", singleThreaded = true)
public class AWSS3ApiMetadataTest {

   @Test(timeOut = 30_000)
   public void testMetadataConstructionNeverContactsTheInstanceMetadataService() throws IOException {
      assertTrue(AWSCredentialsProvider.isAwsSdkAvailable(), "the AWS SDK must be on the test classpath");
      if (System.getenv("AWS_REGION") != null) {
         throw new SkipException("AWS_REGION is set in the environment; the instance metadata path is not reachable");
      }
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

   /** Same stand-in as in AWSCredentialsProviderTest: accepts every connection, never answers. */
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
}
