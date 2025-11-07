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
package org.jclouds.aws.rds;

import java.net.URI;

import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.internal.BaseProviderMetadata;

import com.google.auto.service.AutoService;

/**
 * Implementation of {@link ProviderMetadata} for Amazon RDS with IAM authentication.
 */
@AutoService(ProviderMetadata.class)
public class AWSRdsProviderMetadata extends BaseProviderMetadata {

   public static Builder builder() {
      return new Builder();
   }

   @Override
   public Builder toBuilder() {
      return builder().fromProviderMetadata(this);
   }

   public AWSRdsProviderMetadata() {
      super(builder());
   }

   public AWSRdsProviderMetadata(Builder builder) {
      super(builder);
   }

   public static class Builder extends BaseProviderMetadata.Builder {

      protected Builder() {
         id("aws-rds")
         .name("Amazon Relational Database Service (RDS)")
         .apiMetadata(new AWSRdsApiMetadata())
         .homepage(URI.create("https://aws.amazon.com/rds/"))
         .console(URI.create("https://console.aws.amazon.com/rds/home"))
         .linkedServices("aws-ec2", "aws-s3", "aws-cloudwatch", "aws-rds")
         .iso3166Codes("US", "US-OH", "US-CA", "US-OR", "CA", "BR-SP",
               "IE", "GB-LND", "FR-IDF", "DE-HE", "SE-AB",
               "SG", "AU-NSW", "IN-MH", "JP-13", "KR-11",
               "CN-BJ", "CN-NX", "BH");
      }

      @Override
      public AWSRdsProviderMetadata build() {
         return new AWSRdsProviderMetadata(this);
      }

      @Override
      public Builder fromProviderMetadata(ProviderMetadata in) {
         super.fromProviderMetadata(in);
         return this;
      }
   }
}
