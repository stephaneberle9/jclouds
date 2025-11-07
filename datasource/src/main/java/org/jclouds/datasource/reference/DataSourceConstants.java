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
package org.jclouds.datasource.reference;

/**
 * Configuration properties and constants for DataSource.
 */
public final class DataSourceConstants {

   /**
    * Property for connection pool maximum size.
    * Default: 10
    */
   public static final String PROPERTY_DATASOURCE_MAX_POOL_SIZE = "jclouds.datasource.max-pool-size";

   /**
    * Property for connection pool minimum idle connections.
    * Default: 2
    */
   public static final String PROPERTY_DATASOURCE_MIN_IDLE = "jclouds.datasource.min-idle";

   /**
    * Property for connection timeout in milliseconds.
    * Default: 30000 (30 seconds)
    */
   public static final String PROPERTY_DATASOURCE_CONNECTION_TIMEOUT = "jclouds.datasource.connection-timeout";

   /**
    * Property for maximum connection lifetime in milliseconds.
    * Default: 1800000 (30 minutes)
    */
   public static final String PROPERTY_DATASOURCE_MAX_LIFETIME = "jclouds.datasource.max-lifetime";

   /**
    * Property for idle connection timeout in milliseconds.
    * Default: 600000 (10 minutes)
    */
   public static final String PROPERTY_DATASOURCE_IDLE_TIMEOUT = "jclouds.datasource.idle-timeout";

   /**
    * Property for additional JDBC connection properties as comma-separated key=value pairs.
    * Example: "useSSL=true,requireSSL=true"
    */
   public static final String PROPERTY_DATASOURCE_JDBC_PROPERTIES = "jclouds.datasource.jdbc-properties";

   private DataSourceConstants() {
      throw new AssertionError("intentionally unimplemented");
   }
}
