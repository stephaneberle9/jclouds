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
package org.jclouds.logging;

/**
 * JClouds log abstraction layer.
 * <p/>
 * Implementations of logging are optional and injected if they are configured.
 * <p/>
 * <code> @Resource Logger logger = Logger.NULL;</code> The above will get you a null-safe instance
 * of <tt>Logger</tt>. If configured, this logger will be swapped with a real Logger implementation
 * with category set to the current class name. This is done post-object construction, so do not
 * attempt to use these loggers in your constructor.
 * <p/>
 * If you wish to initialize loggers like these yourself, do not use the @Resource annotation.
 * <p/>
 * This implementation first checks to see if the level is enabled before issuing the log command.
 * In other words, don't do the following
 * <code>if (logger.isTraceEnabled()) logger.trace("message");.
 * <p/>
 */
public interface Logger {

   /**
    * Assign to member to avoid NPE when no logging module is configured.
    */
   Logger NULL = new NullLogger();

   /**
    * Assign to member to avoid NPE when no logging module is configured.
    */
   Logger CONSOLE = new ConsoleLogger();

   String getCategory();

   void trace(String message, Object... args);

   boolean isTraceEnabled();

   void debug(String message, Object... args);

   boolean isDebugEnabled();

   void info(String message, Object... args);

   boolean isInfoEnabled();

   void warn(String message, Object... args);

   void warn(Throwable throwable, String message, Object... args);

   boolean isWarnEnabled();

   void error(String message, Object... args);

   void error(Throwable throwable, String message, Object... args);

   boolean isErrorEnabled();

   /**
    * Produces instances of {@link Logger} relevant to the specified category
    *
    *
    */
   public interface LoggerFactory {
      Logger getLogger(String category);
   }

   /**
    * Helper method to get caller source location for logging.
    * Returns a formatted string like "[org.jclouds.aws.s3.AWSCredentialsProvider:123]"
    *
    * @param stackOffset offset in the stack trace (0 = direct caller, 1 = caller's caller, etc.)
    * @return formatted source location string
    */
   static String getSourceLocation(int stackOffset) {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      // Index: 0=getStackTrace, 1=getSourceLocation, 2=calling method, 3+=actual callers
      int index = 2 + stackOffset;
      if (stackTrace.length > index) {
         StackTraceElement caller = stackTrace[index];
         return String.format("[%s:%d]", caller.getClassName(), caller.getLineNumber());
      }
      return "[unknown]";
   }

   /**
    * Helper method to get caller source location for logging with default offset of 0.
    * Returns a formatted string like "[org.jclouds.aws.s3.AWSCredentialsProvider:123]"
    *
    * @return formatted source location string
    */
   static String getSourceLocation() {
      return getSourceLocation(0);
   }

   /**
    * Helper method to get current timestamp in HH:mm:ss format.
    *
    * @return formatted timestamp string
    */
   static String getTimestamp() {
      java.time.LocalTime now = java.time.LocalTime.now();
      return String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
   }

   /**
    * Formats a log message with timestamp and source location.
    * Returns a string like "21:00:11 [org.jclouds.aws.s3.AWSCredentialsProvider:123]: message"
    *
    * @param message the log message
    * @param stackOffset offset in the stack trace (typically 1 when called from logging method)
    * @return formatted log message with timestamp and source location
    */
   static String formatWithContext(String message, int stackOffset) {
      return getTimestamp() + " " + getSourceLocation(stackOffset + 1) + ": " + message;
   }

   /**
    * Formats a log message with timestamp and source location using default stack offset.
    * Returns a string like "21:00:11 [org.jclouds.aws.s3.AWSCredentialsProvider:123]: message"
    *
    * @param message the log message
    * @return formatted log message with timestamp and source location
    */
   static String formatWithContext(String message) {
      return formatWithContext(message, 0);
   }
}
