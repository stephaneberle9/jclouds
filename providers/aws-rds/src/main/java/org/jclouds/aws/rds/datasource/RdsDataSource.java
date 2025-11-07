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
package org.jclouds.aws.rds.datasource;

import org.jclouds.aws.rds.auth.RdsDbAuthTokenGenerator;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;

/**
 * HikariCP DataSource for AWS RDS with support for IAM authentication.
 *
 * <p>This DataSource automatically enables IAM authentication when no password is provided.
 * Tokens are valid for 15 minutes and are generated on-demand when HikariCP requests credentials.
 *
 * <p>Usage with static password:
 * <pre>
 * RdsDataSource ds = new RdsDataSource();
 * ds.setJdbcUrl("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb");
 * ds.setUsername("dbuser");
 * ds.setPassword("mypassword");  // Static password
 * </pre>
 *
 * <p>Usage with IAM authentication:
 * <pre>
 * RdsDataSource ds = new RdsDataSource();
 * ds.setJdbcUrl("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb");
 * ds.setUsername("dbuser");
 * ds.setPassword(null);  // or empty string - triggers IAM auth
 * </pre>
 */
public class RdsDataSource extends HikariDataSource {

   private RdsDbAuthTokenGenerator authTokenGenerator;

   @Override
   public void setPassword(String password) {
      if (password == null || password.isEmpty()) {
         // Empty password triggers IAM authentication
         createAuthTokenGenerator();
      } else {
         // Static password - use parent implementation
         super.setPassword(password);
      }
   }

   /**
    * Creates and configures the authentication token generator for connecting
    * to an AWS RDS database using IAM credentials.
    * Called automatically when setPassword(null) or setPassword("") is called.
    */
   protected void createAuthTokenGenerator() {
      this.authTokenGenerator = RdsDbAuthTokenGenerator.create();
   }

   @Override
   public Credentials getCredentials() {
      if (authTokenGenerator != null) {
         // Generate fresh authentication token for IAM-based connection
         String username = getUsername();
         String jdbcUrl = getJdbcUrl();
         String token = authTokenGenerator.generateToken(jdbcUrl, username);
         return Credentials.of(username, token);
      }
      // Fall back to static credentials
      return super.getCredentials();
   }
}
