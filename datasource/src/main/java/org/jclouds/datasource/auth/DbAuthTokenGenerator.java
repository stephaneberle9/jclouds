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
package org.jclouds.datasource.auth;

/**
 * Interface for generating authentication tokens for database connections.
 *
 * <p>Implementations of this interface provide authentication tokens that can be used
 * instead of static passwords when connecting to databases with token-based authentication
 * (e.g., AWS RDS IAM authentication, Azure Entra ID authentication).
 *
 * <p>Tokens are typically short-lived (15 minutes to 1 hour) and should be regenerated
 * for each new connection.
 */
public interface DbAuthTokenGenerator {

   /**
    * Generates an authentication token for database connection.
    *
    * <p>The token generation may require:
    * <ul>
    *   <li>Cloud provider credentials (AWS, Azure, etc.)</li>
    *   <li>Database connection information (host, port, username)</li>
    *   <li>Regional or scope-based configuration</li>
    * </ul>
    *
    * <p>Implementations should handle all necessary context internally
    * (e.g., JDBC URL parsing, region detection, credential chain resolution).
    *
    * @return authentication token that can be used as a database password
    */
   String generateToken();
}
