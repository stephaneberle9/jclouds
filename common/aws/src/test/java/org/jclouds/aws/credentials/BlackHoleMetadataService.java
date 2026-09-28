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

import org.testng.SkipException;

/**
 * Stands in for the EC2 instance metadata service in tests: accepts every connection and never
 * answers, which is what 169.254.169.254 looks like from anywhere but EC2. While open it points the
 * AWS SDK at itself ({@code aws.ec2MetadataServiceEndpoint}) and takes every configured region
 * source away ({@code aws.region} cleared, profile and credentials files empty); {@link #close()}
 * restores all of it.
 * <p>
 * Those are JVM-wide system properties. Tests using this class must not run concurrently with
 * tests that resolve ambient AWS credentials or read those files: the modules run them in a
 * surefire execution of their own without parallelism (see the {@code instance-metadata}
 * execution in their POMs, matched by the {@code *InstanceMetadataTest} class name), and they
 * {@link #skipUnlessTheInstanceMetadataPathIsReachable() skip} where {@code AWS_REGION} is in the
 * environment, which no test can unset.
 */
public final class BlackHoleMetadataService implements AutoCloseable {
   private final ServerSocket socket;
   private final List<Socket> held = Collections.synchronizedList(new ArrayList<Socket>());
   private final AtomicInteger connections = new AtomicInteger();
   private final Map<String, String> savedProperties = new HashMap<String, String>();

   public BlackHoleMetadataService() throws IOException {
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

   /**
    * The SDK's region chain reads AWS_REGION before anything else, and a test cannot unset an
    * environment variable: where it names a region the metadata service is never reached, with
    * or without a fix, and an assertion about it would prove nothing.
    */
   public static void skipUnlessTheInstanceMetadataPathIsReachable() {
      if (System.getenv("AWS_REGION") != null) {
         throw new SkipException("AWS_REGION is set in the environment; the instance metadata path is not reachable");
      }
   }

   /** Connections accepted so far: every attempt the SDK made to reach the "metadata service". */
   public int connections() {
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
