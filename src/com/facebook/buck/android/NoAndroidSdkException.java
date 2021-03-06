/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android;


import com.facebook.buck.util.HumanReadableException;

@SuppressWarnings("serial")
public class NoAndroidSdkException extends HumanReadableException {

  private static final String DEFAULT_MESSAGE =
      "Must set ANDROID_SDK to point to the absolute path of your Android SDK directory.";

  public NoAndroidSdkException() {
    super(DEFAULT_MESSAGE);
  }

  private NoAndroidSdkException(String message) {
    super(message);
  }

  public static NoAndroidSdkException createExceptionForPlatformThatCannotBeFound(String platform) {
    String messagePrefix = String.format("The Android SDK for '%s' could not be found. ", platform);
    return new NoAndroidSdkException(messagePrefix + DEFAULT_MESSAGE);
  }
}
