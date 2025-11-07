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
package org.jclouds.datasource.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.jclouds.Context;
import org.jclouds.datasource.DataSourceContext;

import com.google.common.reflect.TypeToken;

/**
 * Default implementation of DataSourceContext for pure JDBC providers.
 *
 * <p>This implementation is suitable for database providers that provide only JDBC access
 * without a REST API backend (like RDS, Azure SQL, etc.). These providers only expose
 * a DataSource via JDBC and don't need the full jclouds backend Context infrastructure.
 */
@Singleton
public class DataSourceContextImpl implements DataSourceContext {

   private final DataSource dataSource;

   @Inject
   public DataSourceContextImpl(DataSource dataSource) {
      this.dataSource = checkNotNull(dataSource, "dataSource");
   }

   @Override
   public DataSource getDataSource() {
      return dataSource;
   }

   @Override
   public void close() throws IOException {
      // Close the DataSource if it's closeable (HikariDataSource implements Closeable)
      if (dataSource instanceof Closeable) {
         ((Closeable) dataSource).close();
      }
   }

   @Override
   public TypeToken<?> getBackendType() {
      // Pure JDBC providers have no backend - return DataSourceContext type
      return TypeToken.of(DataSourceContext.class);
   }

   @Override
   public <C extends Context> C unwrap(TypeToken<C> type) throws IllegalArgumentException {
      throw new UnsupportedOperationException(
         "DataSourceContext does not have a backend Context. " +
         "This is a pure JDBC provider without a jclouds backend.");
   }

   @Override
   public <C extends Context> C unwrap() throws ClassCastException {
      throw new UnsupportedOperationException(
         "DataSourceContext does not have a backend Context. " +
         "This is a pure JDBC provider without a jclouds backend.");
   }

   @Override
   public <A extends Closeable> A unwrapApi(Class<A> apiClass) {
      throw new UnsupportedOperationException(
         "DataSourceContext does not support API unwrapping. " +
         "This is a pure JDBC provider without a REST API backend.");
   }
}
