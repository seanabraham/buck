/*
 * Copyright 2016-present Facebook, Inc.
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

package com.facebook.buck.apple;

import com.facebook.buck.rules.AbstractDescriptionArg;
import com.facebook.infer.annotation.SuppressFieldNotInitialized;

import java.nio.file.Path;

/**
 * A description for a resource defined by a single path.
 */
@SuppressFieldNotInitialized
public class AppleWrapperResourceArg extends AbstractDescriptionArg {
  public Path path;
}
