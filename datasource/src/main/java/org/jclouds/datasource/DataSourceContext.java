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

import java.io.Closeable;

import javax.sql.DataSource;

import org.jclouds.View;
import org.jclouds.datasource.internal.DataSourceContextImpl;

import com.google.inject.ImplementedBy;

/**
 * Represents a connection to a relational database via JDBC DataSource.
 *
 * <p>This context provides access to a {@link DataSource} that can be configured
 * for various database providers with support for dynamic credential generation.
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * DataSourceContext context = ContextBuilder.newBuilder("aws-rds")
 *     .endpoint("jdbc:mysql://mydb.us-east-1.rds.amazonaws.com:3306/mydb")
 *     .credentials("myuser", "")  // Empty password triggers dynamic auth
 *     .buildView(DataSourceContext.class);
 *
 * DataSource dataSource = context.getDataSource();
 * try (Connection conn = dataSource.getConnection()) {
 *     // Use connection
 * }
 *
 * context.close();
 * </pre>
 */
@ImplementedBy(DataSourceContextImpl.class)
public interface DataSourceContext extends View, Closeable {

   /**
    * Returns the configured DataSource.
    *
    * <p>The DataSource is backed by HikariCP connection pool and may support
    * provider-specific authentication mechanisms.
    *
    * @return the DataSource instance
    */
   DataSource getDataSource();

}
