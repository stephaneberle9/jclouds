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
package org.jclouds;

import static com.google.common.base.Suppliers.ofInstance;
import static org.jclouds.providers.AnonymousProviderMetadata.forApiOnEndpoint;
import static org.testng.Assert.assertEquals;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jclouds.apis.ApiMetadata;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.domain.Credentials;
import org.jclouds.events.config.EventBusModule;
import org.jclouds.http.IntegrationTestClient;
import org.jclouds.http.config.ConfiguresHttpCommandExecutorService;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.location.Provider;
import org.jclouds.logging.config.LoggingModule;
import org.jclouds.logging.config.NullLoggingModule;
import org.jclouds.logging.jdk.config.JDKLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.rest.annotations.ApiVersion;
import org.jclouds.rest.config.CredentialStoreModule;
import org.jclouds.rest.internal.BaseHttpApiMetadata;
import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

/**
 * Tests behavior of modules configured in ContextBuilder
 */
@Test(groups = "unit", testName = "ContextBuilderTest")
public class ContextBuilderTest {

   /**
    * Test API metadata class with public builder for testing credentials resolution
    */
   static class TestApiMetadata extends BaseHttpApiMetadata<IntegrationTestClient> {
      public static Builder builder() {
         return new Builder();
      }

      @Override
      public Builder toBuilder() {
         return builder().fromApiMetadata(this);
      }

      private TestApiMetadata(Builder builder) {
         super(builder);
      }

      public static class Builder extends BaseHttpApiMetadata.Builder<IntegrationTestClient, Builder> {
         public Builder() {
            super(IntegrationTestClient.class);
            id("test-api")
            .name("Test API")
            .identityName("identity")
            .defaultIdentity("default-identity")
            .defaultCredential("default-credential")
            .version("1.0")
            .documentation(URI.create("http://test.example.com"));
         }

         @Override
         public TestApiMetadata build() {
            return new TestApiMetadata(this);
         }

         @Override
         protected Builder self() {
            return this;
         }
      }
   }

   /**
    * Test API metadata class WITHOUT default credentials - for testing supplier-only scenarios
    */
   static class TestApiMetadataWithoutDefaults extends BaseHttpApiMetadata<IntegrationTestClient> {
      public static Builder builder() {
         return new Builder();
      }

      @Override
      public Builder toBuilder() {
         return builder().fromApiMetadata(this);
      }

      private TestApiMetadataWithoutDefaults(Builder builder) {
         super(builder);
      }

      public static class Builder extends BaseHttpApiMetadata.Builder<IntegrationTestClient, Builder> {
         public Builder() {
            super(IntegrationTestClient.class);
            id("test-api-no-defaults")
            .name("Test API without default credentials")
            .identityName("identity")
            .version("1.0")
            .documentation(URI.create("http://test.example.com"));
            // Explicitly NOT setting defaultIdentity or defaultCredential
         }

         @Override
         public TestApiMetadataWithoutDefaults build() {
            return new TestApiMetadataWithoutDefaults(this);
         }

         @Override
         protected Builder self() {
            return this;
         }
      }
   }

   @ConfiguresHttpCommandExecutorService
   static class HttpModule extends AbstractModule {
      protected void configure() {
      }
   }

   private ContextBuilder testContextBuilder() {
      return ContextBuilder.newBuilder(forApiOnEndpoint(IntegrationTestClient.class, "http://localhost"));
   }

   @Test
   public void testVariablesReplaceOnEndpoint() {
      ContextBuilder withVariablesToReplace = testContextBuilder().endpoint("http://${jclouds.identity}.service.com")
               .credentials("foo", "bar");
      URI endpoint = withVariablesToReplace.buildInjector().getInstance(
               Key.get(new TypeLiteral<Supplier<URI>>(){}, Provider.class)).get();
      assertEquals(endpoint, URI.create("http://foo.service.com"));
   }

   @Test
   public void testContextName() {
     ContextBuilder withNoName = testContextBuilder().endpoint("http://${jclouds.identity}.service.com").name("mytest")
              .credentials("foo", "bar");
     Context context = withNoName.build();
     assertEquals(context.getName(), "mytest");
   }

   @Test
   public void testProviderMetadataBoundWithCorrectEndpoint() {
      ContextBuilder withVariablesToReplace = testContextBuilder().endpoint("http://${jclouds.identity}.service.com")
               .credentials("foo", "bar");
      String endpoint = withVariablesToReplace.buildInjector().getInstance(ProviderMetadata.class).getEndpoint();
      assertEquals(endpoint, "http://foo.service.com");
   }

   @Test
   public void testProviderMetadataWithEmptyIsoCodePropertyHasEmptySet() {
      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_ISO3166_CODES, "");
      ContextBuilder withVariablesToReplace = testContextBuilder().overrides(overrides).credentials("foo", "bar");
      Set<String> codes = withVariablesToReplace.buildInjector().getInstance(ProviderMetadata.class).getIso3166Codes();
      assertEquals(codes, ImmutableSet.<String> of());
   }

   @Test
   public void testProviderMetadataWithCredentialsSetViaProperty() {
      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_IDENTITY, "foo");
      overrides.setProperty(Constants.PROPERTY_CREDENTIAL, "BAR");
      ContextBuilder withCredsInProps = testContextBuilder().overrides(overrides);
      Credentials creds = withCredsInProps.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {
            }, Provider.class)).get();
      assertEquals(creds, new Credentials("foo", "BAR"));
   }

   @Test
   public void testProviderMetadataWithCredentialsSetSupplier() {
      ContextBuilder withCredsSupplier = testContextBuilder().credentialsSupplier(
            ofInstance(new Credentials("foo", "BAR")));
      Credentials creds = withCredsSupplier.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {
            }, Provider.class)).get();
      assertEquals(creds, new Credentials("foo", "BAR"));
   }
   
   @Test
   public void testProviderMetadataWithVersionSetViaProperty() {
      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_API_VERSION, "1.1");
      ContextBuilder withVersionInProps = testContextBuilder().overrides(overrides);
      String version = withVersionInProps.buildInjector().getInstance(Key.get(String.class, ApiVersion.class));
      assertEquals(version, "1.1");
   }

   @Test
   public void testAllPropertiesAreStrings() {
      Properties overrides = new Properties();
      overrides.setProperty("foo", "bar");
      overrides.put("one", 1);
      overrides.put("two", 2.0f);
      overrides.put("true", true);
      overrides.put("object", new Object() {
         @Override
         public String toString() {
            return "object";
         }
      });
      Context withObjectsInProps = testContextBuilder().overrides(overrides).build();
      Properties resolved = withObjectsInProps.getProviderMetadata().getDefaultProperties();
      assertEquals(resolved.getProperty("foo"), "bar");
      assertEquals(resolved.getProperty("one"), "1");
      assertEquals(resolved.getProperty("true"), "true");
      assertEquals(resolved.getProperty("object"), "object");
   }

   @Test
   public void testAddHttpModuleIfNotPresent() {
      List<Module> modules = Lists.newArrayList();
      HttpModule module = new HttpModule();
      modules.add(module);
      ContextBuilder.addHttpModuleIfNeededAndNotPresent(modules);
      assertEquals(modules.size(), 1);
      assertEquals(modules.remove(0), module);
   }

   @Test
   public void testAddLoggingModuleIfNotPresent() {
      List<Module> modules = Lists.newArrayList();
      LoggingModule module = new NullLoggingModule();
      modules.add(module);
      ContextBuilder.addLoggingModuleIfNotPresent(modules);
      assertEquals(modules.size(), 1);
      assertEquals(modules.remove(0), module);
   }
   
   @Test
   public void testAddEventBusModuleIfNotPresent() {
      List<Module> modules = Lists.newArrayList();
      EventBusModule module = new EventBusModule();
      modules.add(module);
      ContextBuilder.addEventBusIfNotPresent(modules);
      assertEquals(modules.size(), 1);
      assertEquals(modules.remove(0), module);
   }

   @Test
   public void testAddExecutorServiceModuleIfNotPresent() {
      List<Module> modules = Lists.newArrayList();
      ExecutorServiceModule module = new ExecutorServiceModule();
      modules.add(module);
      ContextBuilder.addExecutorServiceIfNotPresent(modules);
      assertEquals(modules.size(), 1);
      assertEquals(modules.remove(0), module);
   }

   @Test
   public void testAddCredentialStoreModuleIfNotPresent() {
      List<Module> modules = Lists.newArrayList();
      CredentialStoreModule module = new CredentialStoreModule();
      modules.add(module);
      ContextBuilder.addCredentialStoreIfNotPresent(modules);
      assertEquals(modules.size(), 1);
      assertEquals(modules.remove(0), module);
   }

   @Test
   public void testAddNone() {
      List<Module> modules = Lists.newArrayList();
      LoggingModule loggingModule = new NullLoggingModule();
      modules.add(loggingModule);
      HttpModule httpModule = new HttpModule();
      modules.add(httpModule);
      ContextBuilder.addHttpModuleIfNeededAndNotPresent(modules);
      ContextBuilder.addLoggingModuleIfNotPresent(modules);
      assertEquals(modules.size(), 2);
      assertEquals(modules.remove(0), loggingModule);
      assertEquals(modules.remove(0), httpModule);
   }

   @Test
   public void testAddBothWhenDefault() {
      List<Module> modules = Lists.newArrayList();
      ContextBuilder.addHttpModuleIfNeededAndNotPresent(modules);
      ContextBuilder.addLoggingModuleIfNotPresent(modules);
      assertEquals(modules.size(), 2);
      assert modules.remove(0) instanceof JavaUrlHttpCommandExecutorServiceModule;
      assert modules.remove(0) instanceof JDKLoggingModule;
   }

   @Test
   public void testAddBothWhenLive() {
      List<Module> modules = Lists.newArrayList();
      ContextBuilder.addHttpModuleIfNeededAndNotPresent(modules);
      ContextBuilder.addLoggingModuleIfNotPresent(modules);
      assertEquals(modules.size(), 2);
      assert modules.remove(0) instanceof JavaUrlHttpCommandExecutorServiceModule;
      assert modules.remove(0) instanceof JDKLoggingModule;
   }

   public void testBuilder() {

      Module module1 = new AbstractModule() {
         protected void configure() {
         }
      };
      Module module2 = new AbstractModule() {
         protected void configure() {
         }
      };
      ContextBuilder builder = testContextBuilder();
      builder.modules(Arrays.asList(module1, module2));

   }

   /**
    * Test Priority 1: credentialsSupplier() builder method has highest priority
    * and overrides API metadata static credentials
    */
   @Test
   public void testCredentialsSupplierOverridesApiMetadataStaticCredentials() {
      // Create API metadata with static credentials
      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultIdentity("metadata-identity")
            .defaultCredential("metadata-credential")
            .build();

      ProviderMetadata providerMetadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      // Build with credentialsSupplier
      Supplier<Credentials> runtimeSupplier = ofInstance(new Credentials("supplier-identity", "supplier-credential"));
      ContextBuilder builder = ContextBuilder.newBuilder(providerMetadata)
            .credentialsSupplier(runtimeSupplier);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // credentialsSupplier should win
      assertEquals(creds, new Credentials("supplier-identity", "supplier-credential"));
   }

   /**
    * Test Priority 1: credentialsSupplier() builder method overrides credentials() method
    */
   @Test
   public void testCredentialsSupplierOverridesCredentialsMethod() {
      ContextBuilder builder = testContextBuilder()
            .credentials("method-identity", "method-credential")
            .credentialsSupplier(ofInstance(new Credentials("supplier-identity", "supplier-credential")));

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // credentialsSupplier should win
      assertEquals(creds, new Credentials("supplier-identity", "supplier-credential"));
   }

   /**
    * Test Priority 1: credentialsSupplier() builder method overrides property overrides
    */
   @Test
   public void testCredentialsSupplierOverridesPropertyOverrides() {
      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_IDENTITY, "override-identity");
      overrides.setProperty(Constants.PROPERTY_CREDENTIAL, "override-credential");

      ContextBuilder builder = testContextBuilder()
            .overrides(overrides)
            .credentialsSupplier(ofInstance(new Credentials("supplier-identity", "supplier-credential")));

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // credentialsSupplier should win
      assertEquals(creds, new Credentials("supplier-identity", "supplier-credential"));
   }

   /**
    * Test Priority 1: credentialsSupplier() builder method overrides API metadata credentialsSupplier
    */
   @Test
   public void testCredentialsSupplierOverridesApiMetadataCredentialsSupplier() {
      // Create API metadata with a credentials supplier
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));

      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      // Build with builder credentialsSupplier
      Supplier<Credentials> builderSupplier = ofInstance(new Credentials("builder-supplier-identity", "builder-supplier-credential"));
      ContextBuilder builder = ContextBuilder.newBuilder(metadata)
            .credentialsSupplier(builderSupplier);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // builder credentialsSupplier should win
      assertEquals(creds, new Credentials("builder-supplier-identity", "builder-supplier-credential"));
   }

   /**
    * Test Priority 2: credentials() method overrides API metadata static credentials
    */
   @Test
   public void testCredentialsMethodOverridesApiMetadataStaticCredentials() {
      // Create API metadata with static credentials
      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultIdentity("metadata-identity")
            .defaultCredential("metadata-credential")
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      ContextBuilder builder = ContextBuilder.newBuilder(metadata)
            .credentials("method-identity", "method-credential");

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // credentials() method should win
      assertEquals(creds, new Credentials("method-identity", "method-credential"));
   }

   /**
    * Test Priority 2: credentials() method overrides API metadata credentialsSupplier
    */
   @Test
   public void testCredentialsMethodOverridesApiMetadataCredentialsSupplier() {
      // Create API metadata with a credentials supplier
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));

      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      ContextBuilder builder = ContextBuilder.newBuilder(metadata)
            .credentials("method-identity", "method-credential");

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // credentials() method should win
      assertEquals(creds, new Credentials("method-identity", "method-credential"));
   }

   /**
    * Test Priority 2: property overrides override API metadata static credentials
    */
   @Test
   public void testPropertyOverridesOverrideApiMetadataStaticCredentials() {
      // Create API metadata with static credentials
      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultIdentity("metadata-identity")
            .defaultCredential("metadata-credential")
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_IDENTITY, "override-identity");
      overrides.setProperty(Constants.PROPERTY_CREDENTIAL, "override-credential");

      ContextBuilder builder = ContextBuilder.newBuilder(metadata)
            .overrides(overrides);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // property overrides should win
      assertEquals(creds, new Credentials("override-identity", "override-credential"));
   }

   /**
    * Test Priority 2: property overrides override API metadata credentialsSupplier
    */
   @Test
   public void testPropertyOverridesOverrideApiMetadataCredentialsSupplier() {
      // Create API metadata with a credentials supplier
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));

      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_IDENTITY, "override-identity");
      overrides.setProperty(Constants.PROPERTY_CREDENTIAL, "override-credential");

      ContextBuilder builder = ContextBuilder.newBuilder(metadata)
            .overrides(overrides);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // property overrides should win
      assertEquals(creds, new Credentials("override-identity", "override-credential"));
   }

   /**
    * Test Priority 3: API metadata credentialsSupplier is used when no builder credentials are set
    */
   @Test
   public void testApiMetadataCredentialsSupplierUsedWhenNoBuilderCredentials() {
      // Create API metadata with a credentials supplier
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));

      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      ContextBuilder builder = ContextBuilder.newBuilder(metadata);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // API metadata credentialsSupplier should be used
      assertEquals(creds, new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));
   }

   /**
    * Test Priority 3: API metadata credentialsSupplier is preferred over static credentials from metadata
    */
   @Test
   public void testApiMetadataCredentialsSupplierPreferredOverStaticCredentials() {
      // Create API metadata with both a credentials supplier and static credentials
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));

      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultIdentity("metadata-static-identity")
            .defaultCredential("metadata-static-credential")
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      ContextBuilder builder = ContextBuilder.newBuilder(metadata);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // API metadata credentialsSupplier should win over static credentials
      assertEquals(creds, new Credentials("metadata-supplier-identity", "metadata-supplier-credential"));
   }

   /**
    * Test Priority 3: API metadata credentialsSupplier works without static credentials
    * This test ensures that when API metadata provides ONLY a credentials supplier
    * (no defaultIdentity/defaultCredential), the builder doesn't fail with
    * "property identity not present in properties" error.
    */
   @Test
   public void testApiMetadataCredentialsSupplierOnlyWithoutStaticCredentials() {
      // Create API metadata with ONLY credentials supplier, no static identity/credential
      Supplier<Credentials> metadataSupplier = ofInstance(new Credentials("supplier-only-identity", "supplier-only-credential"));

      // Use TestApiMetadataWithoutDefaults which doesn't set defaultIdentity or defaultCredential
      ApiMetadata apiMetadata = TestApiMetadataWithoutDefaults.builder()
            .defaultCredentialsSupplier(metadataSupplier)
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      // This should NOT throw "property identity not present in properties" error
      // because line 310 in ContextBuilder checks for apiMetadata.getDefaultCredentialsSupplier().isPresent()
      ContextBuilder builder = ContextBuilder.newBuilder(metadata);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // API metadata credentialsSupplier should be used
      assertEquals(creds, new Credentials("supplier-only-identity", "supplier-only-credential"));
   }

   /**
    * Test Priority 4: API metadata static credentials are used as final fallback
    */
   @Test
   public void testApiMetadataStaticCredentialsUsedAsFallback() {
      // Create API metadata with only static credentials
      ApiMetadata apiMetadata = TestApiMetadata.builder()
            .defaultIdentity("metadata-identity")
            .defaultCredential("metadata-credential")
            .build();

      ProviderMetadata metadata = forApiOnEndpoint(IntegrationTestClient.class, "http://localhost")
            .toBuilder()
            .apiMetadata(apiMetadata)
            .build();

      ContextBuilder builder = ContextBuilder.newBuilder(metadata);

      Credentials creds = builder.buildInjector()
            .getInstance(Key.get(new TypeLiteral<Supplier<Credentials>>() {}, Provider.class))
            .get();

      // API metadata static credentials should be used
      assertEquals(creds, new Credentials("metadata-identity", "metadata-credential"));
   }
}
