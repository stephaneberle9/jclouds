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
package org.jclouds.azure.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.jclouds.domain.Credentials;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Azure credentials for API authentication.
 *
 * @see <a href=
 *      "https://learn.microsoft.com/en-us/azure/active-directory/develop/access-tokens"
 *      />
 */
public final class SessionCredentials extends Credentials {

   private final String sessionToken;
   private final Optional<Date> expiration;

   private SessionCredentials(String identity, String credential, String sessionToken, Optional<Date> expiration) {
      super(checkNotNull(identity, "identity"), checkNotNull(credential, "credential for %s",
            identity));
      this.sessionToken = checkNotNull(sessionToken, "sessionToken for %s", identity);
      this.expiration = checkNotNull(expiration, "expiration for %s", identity);
   }

   /**
    * Identity that identifies the credentials (e.g., account name or "azure-token").
    */
   public String getIdentity() {
      return identity;
   }

   /**
    * The credential (e.g., access token or account key).
    */
   public String getCredential() {
      return credential;
   }

   /**
    * The session token (typically the same as credential for Azure access tokens).
    */
   public String getSessionToken() {
      return sessionToken;
   }

   /**
    * The date on which these credentials expire.
    */
   public Optional<Date> getExpiration() {
      return expiration;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(identity, credential, sessionToken, expiration);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SessionCredentials other = (SessionCredentials) obj;
      return Objects.equal(this.identity, other.identity) && Objects.equal(this.credential, other.credential)
            && Objects.equal(this.sessionToken, other.sessionToken) && Objects.equal(this.expiration, other.expiration);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).omitNullValues().add("identity", identity)
            .add("sessionToken", sessionToken).add("expiration", expiration.orNull()).toString();
   }

   public static Builder builder() {
      return new Builder();
   }

   public Builder toBuilder() {
      return builder().from(this);
   }

   public static final class Builder extends Credentials.Builder<SessionCredentials> {
      private String identity;
      private String credential;
      private String sessionToken;
      private Optional<Date> expiration = Optional.absent();

      @Override
      public Builder identity(String identity) {
         this.identity = identity;
         return this;
      }

      @Override
      public Builder credential(String credential) {
         this.credential = credential;
         return this;
      }

      /**
       * @see SessionCredentials#getSessionToken()
       */
      public Builder sessionToken(String sessionToken) {
         this.sessionToken = sessionToken;
         return this;
      }

      /**
       * @see SessionCredentials#getExpiration()
       */
      public Builder expiration(Date expiration) {
         this.expiration = Optional.fromNullable(expiration);
         return this;
      }

      public SessionCredentials build() {
         return new SessionCredentials(identity, credential, sessionToken, expiration);
      }

      public Builder from(SessionCredentials in) {
         return this.identity(in.identity).credential(in.credential).sessionToken(in.sessionToken)
               .expiration(in.expiration.orNull());
      }
   }
}
