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
package org.jclouds.azure.database.datasource;

import jakarta.annotation.Resource;

import org.jclouds.azure.database.auth.AzureDbAuthTokenGenerator;
import org.jclouds.logging.Logger;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;

/**
 * HikariCP DataSource for Azure Database with support for Entra ID authentication.
 *
 * <p>This DataSource automatically enables Entra ID authentication when no password is provided.
 * Tokens are valid for approximately 1 hour and are generated on-demand when HikariCP requests credentials.
 *
 * <p>Usage with static password:
 * <pre>
 * AzureDatabaseDataSource ds = new AzureDatabaseDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.postgres.database.azure.com:5432/mydb?sslmode=require");
 * ds.setUsername("dbuser");
 * ds.setPassword("mypassword");  // Static password
 * </pre>
 *
 * <p>Usage with Entra ID authentication:
 * <pre>
 * AzureDatabaseDataSource ds = new AzureDatabaseDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.postgres.database.azure.com:5432/mydb?sslmode=require");
 * ds.setUsername("dbuser@myserver");  // Note: username format may include @servername
 * ds.setPassword(null);  // or empty string - triggers Entra ID auth
 * </pre>
 */
public class AzureDatabaseDataSource extends HikariDataSource {

   @Resource
   private Logger logger = Logger.NULL;

   private AzureDbAuthTokenGenerator authTokenGenerator;

   @Override
   public void setPassword(String password) {
      if (password == null || password.isEmpty()) {
         logger.debug("Empty password provided - enabling Entra ID authentication mode");
         // Empty password triggers Entra ID authentication
         createAuthTokenGenerator();
      } else {
         logger.debug("Static password provided - using standard password authentication");
         // Static password - use parent implementation
         super.setPassword(password);
      }
   }

   /**
    * Creates and configures the authentication token generator for connecting
    * to an Azure Database using Entra ID credentials.
    * Called automatically when setPassword(null) or setPassword("") is called.
    */
   protected void createAuthTokenGenerator() {
      this.authTokenGenerator = AzureDbAuthTokenGenerator.create();
   }

   @Override
   public Credentials getCredentials() {
      if (authTokenGenerator != null) {
         // Generate fresh authentication token for Entra ID-based connection
         String username = getUsername();
         logger.debug("Generating Entra ID authentication token for user '%s'", username);
         String token = authTokenGenerator.generateToken();
         logger.debug("Entra ID authentication token generated successfully (token length: %d characters)", token.length());
         return Credentials.of(username, token);
      }
      // Fall back to static credentials
      logger.debug("Using static password credentials for user '%s'", getUsername());
      return super.getCredentials();
   }
}
