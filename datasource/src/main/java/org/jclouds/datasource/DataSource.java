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
package org.jclouds.datasource;

import jakarta.annotation.Resource;

import org.jclouds.datasource.auth.DbAuthTokenGenerator;
import org.jclouds.logging.Logger;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;

/**
 * Base HikariCP DataSource with support for token-based authentication.
 *
 * <p>This DataSource automatically enables token-based authentication (e.g., IAM, Entra ID)
 * when no password is provided. Tokens are generated on-demand when HikariCP requests credentials.
 *
 * <p>Subclasses must implement {@link #createAuthTokenGenerator()} to provide the specific
 * token generation logic for their cloud provider.
 *
 * <p>Usage with static password:
 * <pre>
 * DataSource ds = new ConcreteDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.example.com:5432/mydb");
 * ds.setUsername("dbuser");
 * ds.setPassword("mypassword");  // Static password
 * </pre>
 *
 * <p>Usage with token-based authentication:
 * <pre>
 * DataSource ds = new ConcreteDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.example.com:5432/mydb");
 * ds.setUsername("dbuser");
 * ds.setPassword(null);  // or empty string - triggers token-based auth
 * </pre>
 */
public abstract class DataSource extends HikariDataSource {

   @Resource
   protected Logger logger = Logger.NULL;

   private DbAuthTokenGenerator authTokenGenerator;

   @Override
   public void setPassword(String password) {
      if (password == null || password.isEmpty()) {
         logger.debug("Empty password provided - enabling token-based authentication mode");
         // Empty password triggers token-based authentication
         this.authTokenGenerator = createAuthTokenGenerator();
      } else {
         logger.debug("Static password provided - using standard password authentication");
         // Static password - use parent implementation
         this.authTokenGenerator = null;
         super.setPassword(password);
      }
   }

   /**
    * Creates and configures the authentication token generator for this DataSource.
    *
    * <p>This method is called automatically when {@code setPassword(null)} or
    * {@code setPassword("")} is called, triggering token-based authentication mode.
    *
    * <p>Implementations should return a provider-specific token generator (e.g.,
    * AWS RDS IAM, Azure Entra ID) that implements {@link DbAuthTokenGenerator}.
    *
    * @return a DbAuthTokenGenerator instance for generating authentication tokens
    */
   protected abstract DbAuthTokenGenerator createAuthTokenGenerator();

   @Override
   public Credentials getCredentials() {
      if (authTokenGenerator != null) {
         // Generate fresh authentication token for token-based connection
         String username = getUsername();
         logger.debug("Generating authentication token for user '%s'", username);
         String token = authTokenGenerator.generateToken();
         logger.debug("Authentication token generated successfully (token length: %d characters)", token.length());
         return Credentials.of(username, token);
      }
      // Fall back to static credentials
      logger.debug("Using static password credentials for user '%s'", getUsername());
      return super.getCredentials();
   }
}
