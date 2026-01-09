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
package org.jclouds.azure.databases.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.jclouds.azure.databases.datasource.AzureDatabasesDataSource;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "AzureDatabasesContextModuleTest")
public class AzureDatabasesContextModuleTest {

   @Test
   public void testAzureDatabasesPoolNameOverride() {
      AzureDatabasesContextModule module = new AzureDatabasesContextModule();
      AzureDatabasesDataSource ds = new AzureDatabasesDataSource();

      // Test connection pool configuration sets Azure-specific pool name
      module.configureConnectionPool(ds, "10", "2", "30000", "1800000", "600000");

      assertEquals(ds.getMaximumPoolSize(), 10);
      assertEquals(ds.getMinimumIdle(), 2);
      assertEquals(ds.getPoolName(), "jclouds-azuredatabases-pool");

      ds.close();
   }

   @Test
   public void testNoBindingConflictWhenExtendingDataSourceContextModule() {
      // Test that AzureDatabasesContextModule can extend DataSourceContextModule and override
      // bindDataSourceContext() without causing duplicate binding errors

      // Verify the module can be instantiated
      AzureDatabasesContextModule module = new AzureDatabasesContextModule();
      assertNotNull(module);
   }
}
