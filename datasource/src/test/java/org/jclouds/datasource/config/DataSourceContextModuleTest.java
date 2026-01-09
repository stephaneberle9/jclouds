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

import org.jclouds.datasource.DataSource;
import org.jclouds.datasource.DataSourceContext;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

@Test(groups = "unit", testName = "DataSourceContextModuleTest")
public class DataSourceContextModuleTest {

   @Test
   public void testTemplateMethods() {
      // Test that template methods can be called
      DataSourceContextModule module = new DataSourceContextModule();

      DataSource ds = module.createDataSource();
      assertNotNull(ds);

      // Test configureCredentials
      org.jclouds.domain.Credentials creds = new org.jclouds.domain.Credentials("user", "pass");
      module.configureCredentials(ds, creds, "jdbc:h2:mem:test");

      // Test configureConnectionPool
      module.configureConnectionPool(ds, "5", "1", "10000", "600000", "300000");
      assertEquals(ds.getMaximumPoolSize(), 5);
      assertEquals(ds.getMinimumIdle(), 1);

      ds.close();
   }

   @Test
   public void testProvideDataSourceContextCanBeOverridden() {
      // Test that subclasses can override provideDataSourceContext()
      class CustomDataSourceContextModule extends DataSourceContextModule {
         @Override
         protected DataSourceContext provideDataSourceContext(javax.sql.DataSource dataSource) {
            // Override provides method
            return super.provideDataSourceContext(dataSource);
         }
      }

      // Verify the module can be instantiated
      CustomDataSourceContextModule module = new CustomDataSourceContextModule();
      assertNotNull(module);
   }
}
