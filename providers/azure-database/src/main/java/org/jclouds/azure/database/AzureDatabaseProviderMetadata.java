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
package org.jclouds.azure.database;

import java.net.URI;

import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.internal.BaseProviderMetadata;

import com.google.auto.service.AutoService;

/**
 * Implementation of {@link ProviderMetadata} for Azure Database with Entra ID authentication.
 */
@AutoService(ProviderMetadata.class)
public class AzureDatabaseProviderMetadata extends BaseProviderMetadata {

   public static Builder builder() {
      return new Builder();
   }

   @Override
   public Builder toBuilder() {
      return builder().fromProviderMetadata(this);
   }

   public AzureDatabaseProviderMetadata() {
      super(builder());
   }

   public AzureDatabaseProviderMetadata(Builder builder) {
      super(builder);
   }

   public static class Builder extends BaseProviderMetadata.Builder {

      protected Builder() {
         id("azure-database")
         .name("Azure Database for PostgreSQL/MySQL")
         .apiMetadata(new AzureDatabaseApiMetadata())
         .homepage(URI.create("https://azure.microsoft.com/en-us/products/category/databases"))
         .console(URI.create("https://portal.azure.com/#browse/Microsoft.DBforPostgreSQL%2Fservers"))
         .linkedServices("azurecompute-arm", "azureblob", "azure-database")
         .iso3166Codes("US", "US-VA", "US-CA", "US-TX", "US-AZ", "US-WA",
               "CA", "CA-QC", "BR-SP",
               "IE", "GB-LND", "FR-IDF", "DE-HE", "NL", "SE", "NO", "CH", "PL",
               "SG", "AU-NSW", "IN-MH", "JP-13", "KR-11",
               "ZA");
      }

      @Override
      public AzureDatabaseProviderMetadata build() {
         return new AzureDatabaseProviderMetadata(this);
      }

      @Override
      public Builder fromProviderMetadata(ProviderMetadata in) {
         super.fromProviderMetadata(in);
         return this;
      }
   }
}
