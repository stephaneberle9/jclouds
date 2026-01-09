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
package org.jclouds.aws.rds.auth;

import java.net.URI;

import org.jclouds.datasource.auth.DbAuthTokenGenerator;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;

/**
 * Generates authentication tokens for connecting to an AWS RDS database using IAM credentials.
 *
 * <p>This utility class creates authentication tokens that can be used instead
 * of passwords when connecting to RDS databases with IAM authentication enabled.
 *
 * <p>The tokens are valid for 15 minutes and should be regenerated for each
 * new connection.
 */
public class RdsDbAuthTokenGenerator implements DbAuthTokenGenerator {

   private RdsUtilities rdsUtilities;
   private final AwsCredentialsProvider credentialsProvider;
   private final URI uri;
   private final String username;
   private Region region;

   /**
    * Creates a new token generator for a specific connection using default AWS credentials and region.
    * Region detection is deferred until the first token generation request.
    *
    * @param jdbcUrl JDBC URL (e.g., "jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
    * @param username database username
    * @return a new RdsDbAuthTokenGenerator instance
    */
   public static RdsDbAuthTokenGenerator forConnection(String jdbcUrl, String username) {
      URI uri = parseJdbcUrl(jdbcUrl);
      return new RdsDbAuthTokenGenerator(DefaultCredentialsProvider.create(), uri, username, null);
   }

   /**
    * Parses a JDBC URL to extract the hostname and port.
    *
    * @param jdbcUrl JDBC URL
    * @return URI with host and port information
    */
   protected static URI parseJdbcUrl(String jdbcUrl) {
      // Remove "jdbc:" prefix to get a standard URI
      // e.g., "jdbc:mysql://host:port/db" -> "mysql://host:port/db"
      String uriString = jdbcUrl.substring(5);
      return URI.create(uriString);
   }

   /**
    * Creates a new token generator with specific credentials provider and region.
    *
    * @param credentialsProvider AWS credentials provider
    * @param uri parsed URI from JDBC URL
    * @param username database username
    * @param region AWS region (can be null to use default region provider chain)
    */
   public RdsDbAuthTokenGenerator(AwsCredentialsProvider credentialsProvider, URI uri, String username, Region region) {
      this.credentialsProvider = credentialsProvider;
      this.uri = uri;
      this.username = username;
      this.region = region;
   }

   /**
    * Initializes RdsUtilities with credentials provider and region.
    * Called lazily on first token generation.
    * Protected to allow subclasses to customize initialization if needed.
    */
   protected synchronized void initializeRdsUtilities() {
      if (rdsUtilities == null) {
         if (region == null) {
            // Detect region using default provider chain
            region = new DefaultAwsRegionProviderChain().getRegion();
         }
         rdsUtilities = RdsUtilities.builder()
                 .credentialsProvider(credentialsProvider)
                 .region(region)
                 .build();
      }
   }

   /**
    * Generates an authentication token for connecting to an AWS RDS database using IAM credentials.
    *
    * @return authentication token valid for 15 minutes
    */
   @Override
   public String generateToken() {
      // Ensure RdsUtilities is initialized (lazy initialization)
      if (rdsUtilities == null) {
         initializeRdsUtilities();
      }

      return rdsUtilities.generateAuthenticationToken(builder -> builder
              .hostname(uri.getHost())
              .port(uri.getPort() > 0 ? uri.getPort() : 3306)
              .username(username)
              .region(region));
   }
}
