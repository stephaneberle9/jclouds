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
package org.jclouds.azure.database.config;

import org.jclouds.azure.database.datasource.AzureDatabaseDataSource;
import org.jclouds.datasource.config.DataSourceContextModule;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Guice module for configuring Azure Database context with support for Entra ID authentication.
 *
 * <p>This module extends the generic DataSource module to provide Azure Database-specific
 * functionality including support for Entra ID authentication via dynamic token generation.
 *
 * <p>The AzureDatabaseDataSource automatically detects when Entra ID authentication should be used
 * (when no password is provided) and generates authentication tokens on-demand.
 */
public class AzureDatabaseContextModule extends DataSourceContextModule {

   @Override
   protected HikariDataSource createDataSource() {
      return new AzureDatabaseDataSource();
   }

   @Override
   protected void configureConnectionPool(HikariDataSource dataSource,
         String maxPoolSize, String minIdle, String connectionTimeout,
         String maxLifetime, String idleTimeout) {
      super.configureConnectionPool(dataSource, maxPoolSize, minIdle, connectionTimeout, maxLifetime, idleTimeout);
      // Override pool name for Azure-specific pool
      dataSource.setPoolName("jclouds-azure-database-pool");
   }
}
