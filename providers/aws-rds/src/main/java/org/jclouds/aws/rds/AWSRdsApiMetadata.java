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

import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_CONNECTION_TIMEOUT;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_IDLE_TIMEOUT;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MAX_LIFETIME;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MAX_POOL_SIZE;
import static org.jclouds.datasource.reference.DataSourceConstants.PROPERTY_DATASOURCE_MIN_IDLE;

import java.net.URI;
import java.util.Properties;

import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.internal.BaseApiMetadata;
import org.jclouds.aws.rds.config.AWSRdsContextModule;
import org.jclouds.datasource.DataSourceContext;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.inject.Module;

/**
 * Implementation of {@link ApiMetadata} for AWS RDS with support for IAM authentication.
 *
 * <p>Note: AWS credentials for generating authentication tokens are obtained via AWS SDK
 * DefaultCredentialsProvider inside RdsDbAuthTokenGenerator, not through jclouds
 * credential system. The jclouds credentials are used for database username/password.
 */
@AutoService(ApiMetadata.class)
public class AWSRdsApiMetadata extends BaseApiMetadata {

   @Override
   public Builder toBuilder() {
      return new Builder().fromApiMetadata(this);
   }

   public AWSRdsApiMetadata() {
      this(new Builder());
   }

   protected AWSRdsApiMetadata(Builder builder) {
      super(builder);
   }

   public static Properties defaultProperties() {
      Properties properties = BaseApiMetadata.defaultProperties();
      properties.setProperty(PROPERTY_DATASOURCE_MAX_POOL_SIZE, "10");
      properties.setProperty(PROPERTY_DATASOURCE_MIN_IDLE, "2");
      properties.setProperty(PROPERTY_DATASOURCE_CONNECTION_TIMEOUT, "30000");
      properties.setProperty(PROPERTY_DATASOURCE_MAX_LIFETIME, "1800000");
      properties.setProperty(PROPERTY_DATASOURCE_IDLE_TIMEOUT, "600000");
      return properties;
   }

   public static class Builder extends BaseApiMetadata.Builder<Builder> {

      protected Builder() {
         id("aws-rds")
         .name("Amazon RDS with IAM Authentication")
         .identityName("Database Username")
         .credentialName("Database Password (leave empty for IAM auth)")
         .defaultEndpoint("jdbc:mysql://localhost:3306/mydb")
         .version("1.0")
         .documentation(URI.create("https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html"))
         .defaultProperties(AWSRdsApiMetadata.defaultProperties())
         .view(TypeToken.of(DataSourceContext.class))
         .defaultModules(ImmutableSet.<Class<? extends Module>>of(AWSRdsContextModule.class));
      }

      @Override
      public AWSRdsApiMetadata build() {
         return new AWSRdsApiMetadata(this);
      }

      @Override
      protected Builder self() {
         return this;
      }
   }
}
