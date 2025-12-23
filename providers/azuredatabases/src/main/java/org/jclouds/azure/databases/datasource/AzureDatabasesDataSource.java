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
package org.jclouds.azure.databases.datasource;

import org.jclouds.azure.databases.auth.AzureDbAuthTokenGenerator;
import org.jclouds.datasource.auth.DbAuthTokenGenerator;

/**
 * HikariCP DataSource for Azure Databases with support for Entra ID authentication.
 *
 * <p>This DataSource automatically enables Entra ID authentication when no password is provided.
 * Tokens are valid for approximately 1 hour and are generated on-demand when HikariCP requests credentials.
 *
 * <p>Usage with static password:
 * <pre>
 * AzureDatabasesDataSource ds = new AzureDatabasesDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.postgres.database.azure.com:5432/mydb?sslmode=require");
 * ds.setUsername("dbuser");
 * ds.setPassword("mypassword");  // Static password
 * </pre>
 *
 * <p>Usage with Entra ID authentication:
 * <pre>
 * AzureDatabasesDataSource ds = new AzureDatabasesDataSource();
 * ds.setJdbcUrl("jdbc:postgresql://myserver.postgres.database.azure.com:5432/mydb?sslmode=require");
 * ds.setUsername("dbuser@myserver");  // Note: username format may include @servername
 * ds.setPassword(null);  // or empty string - triggers Entra ID auth
 * </pre>
 */
public class AzureDatabasesDataSource extends org.jclouds.datasource.DataSource {

   @Override
   protected DbAuthTokenGenerator createAuthTokenGenerator() {
      return AzureDbAuthTokenGenerator.create();
   }
}
