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
package org.jclouds.aws.rds.config;
import org.jclouds.aws.rds.datasource.RdsDataSource;
import org.jclouds.datasource.config.DataSourceContextModule;
import org.jclouds.datasource.DataSource;

/**
 * Guice module for configuring AWS RDS context with support for IAM authentication.
 *
 * <p>This module extends the generic DataSource module to provide AWS RDS-specific
 * functionality including support for IAM authentication via dynamic token generation.
 *
 * <p>The RdsDataSource automatically detects when IAM authentication should be used
 * (when no password is provided) and generates authentication tokens on-demand.
 */
public class AWSRdsContextModule extends DataSourceContextModule {

   @Override
   protected DataSource createDataSource() {
      return new RdsDataSource();
   }

   @Override
   protected void configureConnectionPool(DataSource dataSource,
         String maxPoolSize, String minIdle, String connectionTimeout,
         String maxLifetime, String idleTimeout) {
      super.configureConnectionPool(dataSource, maxPoolSize, minIdle, connectionTimeout, maxLifetime, idleTimeout);
      // Override pool name for AWS-specific pool
      dataSource.setPoolName("jclouds-aws-rds-pool");
   }
}
