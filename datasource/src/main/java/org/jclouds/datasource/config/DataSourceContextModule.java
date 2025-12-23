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
package org.jclouds.datasource.config;

import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_CONNECTION_TIMEOUT;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_IDLE_TIMEOUT;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MAX_LIFETIME;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MAX_POOL_SIZE;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MIN_IDLE;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.jclouds.datasource.DataSourceContext;
import org.jclouds.datasource.internal.DataSourceContextImpl;

import org.jclouds.domain.Credentials;
import org.jclouds.location.Provider;
import org.jclouds.providers.ProviderMetadata;

import com.google.common.base.Supplier;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Names;

/**
 * Guice module for configuring DataSource context.
 *
 * <p>This module binds the {@link DataSourceContext} implementation and provides
 * a configured {@link DataSource} instance backed by HikariCP.
 */
public class DataSourceContextModule extends AbstractModule {

   @Provides
   @Singleton
   protected DataSourceContext provideDataSourceContext(javax.sql.DataSource dataSource) {
      return createDataSourceContext(dataSource);
   }

   /**
    * Creates the DataSourceContext instance. Can be overridden by subclasses to provide
    * a custom DataSourceContext implementation.
    *
    * @param dataSource the DataSource to wrap
    * @return a new DataSourceContext instance
    */
   protected DataSourceContext createDataSourceContext(javax.sql.DataSource dataSource) {
      return new DataSourceContextImpl(dataSource);
   }

   @Provides
   @Singleton
   protected javax.sql.DataSource provideDataSource(
         @Provider Supplier<Credentials> creds,
         ProviderMetadata providerMetadata,
         Injector injector) {

      // NOTE: Retrieve properties manually from Injector instead of using @Named parameters
      // This avoids a Guice issue where @Named parameters in @Provides methods don't see
      // bindings made by Names.bindProperties() from another module
      String maxPoolSize = injector.getInstance(Key.get(String.class, Names.named(PROPERTY_DATASOURCE_MAX_POOL_SIZE)));
      String minIdle = injector.getInstance(Key.get(String.class, Names.named(PROPERTY_DATASOURCE_MIN_IDLE)));
      String connectionTimeout = injector.getInstance(Key.get(String.class, Names.named(PROPERTY_DATASOURCE_CONNECTION_TIMEOUT)));
      String maxLifetime = injector.getInstance(Key.get(String.class, Names.named(PROPERTY_DATASOURCE_MAX_LIFETIME)));
      String idleTimeout = injector.getInstance(Key.get(String.class, Names.named(PROPERTY_DATASOURCE_IDLE_TIMEOUT)));

      // NOTE: Endpoint cannot be retrieved from Injector like the properties above because
      // UpdateProviderMetadataFromProperties extracts and removes it from the Properties object (via getAndRemove)
      // before Names.bindProperties() is called. The endpoint is stored in ProviderMetadata from there on instead.
      String endpoint = providerMetadata.getEndpoint();
      if (endpoint == null || endpoint.isEmpty()) {
         // Fallback to API metadata default endpoint if provider metadata doesn't have one
         endpoint = providerMetadata.getApiMetadata().getDefaultEndpoint().orNull();
      }

      org.jclouds.datasource.DataSource dataSource = createDataSource();

      // Request Guice to inject members (including @Resource Logger) into the manually created DataSource
      injector.injectMembers(dataSource);

      dataSource.setJdbcUrl(endpoint);

      // Configure credentials (can be overridden by subclasses for IAM auth, etc.)
      configureCredentials(dataSource, creds.get(), endpoint);

      // Configure connection pool
      configureConnectionPool(dataSource, maxPoolSize, minIdle, connectionTimeout, maxLifetime, idleTimeout);

      return dataSource;
   }

   /**
    * Creates the DataSource instance. Can be overridden by subclasses to provide
    * a custom DataSource implementation.
    *
    * @return a new DataSource instance
    */
   protected org.jclouds.datasource.DataSource createDataSource() {
      return new org.jclouds.datasource.DataSource() {
         @Override
         protected org.jclouds.datasource.auth.DbAuthTokenGenerator createAuthTokenGenerator() {
            throw new UnsupportedOperationException(
                  "Token-based authentication not supported by default DataSourceContextModule. " +
                  "Use a provider-specific module (e.g., AWSRdsContextModule, AzureDatabasesContextModule) " +
                  "for IAM/Entra ID authentication.");
         }
      };
   }

   /**
    * Configures credentials on the DataSource. This implementation sets static
    * username and password. Subclasses can override this to provide dynamic
    * credential generation (e.g., IAM authentication).
    *
    * @param dataSource the DataSource to configure
    * @param credentials the credentials (username and password)
    * @param endpoint the JDBC endpoint URL
    */
   protected void configureCredentials(org.jclouds.datasource.DataSource dataSource, Credentials credentials, String endpoint) {
      // Set static credentials (username and password)
      dataSource.setUsername(credentials.identity);
      dataSource.setPassword(credentials.credential);
   }

   /**
    * Configures connection pool settings on the DataSource.
    *
    * @param dataSource the DataSource to configure
    * @param maxPoolSize maximum connection pool size
    * @param minIdle minimum idle connections
    * @param connectionTimeout connection timeout in milliseconds
    * @param maxLifetime maximum connection lifetime in milliseconds
    * @param idleTimeout idle connection timeout in milliseconds
    */
   protected void configureConnectionPool(org.jclouds.datasource.DataSource dataSource,
         String maxPoolSize, String minIdle, String connectionTimeout,
         String maxLifetime, String idleTimeout) {
      // Properties are retrieved from ApiMetadata defaults or user overrides
      dataSource.setMaximumPoolSize(Integer.parseInt(maxPoolSize));
      dataSource.setMinimumIdle(Integer.parseInt(minIdle));
      dataSource.setConnectionTimeout(Long.parseLong(connectionTimeout));
      dataSource.setMaxLifetime(Long.parseLong(maxLifetime));
      dataSource.setIdleTimeout(Long.parseLong(idleTimeout));

      // Recommended settings for database connection pools
      dataSource.setPoolName("jclouds-datasource-pool");
      dataSource.setLeakDetectionThreshold(60000); // 60 seconds
   }
}
