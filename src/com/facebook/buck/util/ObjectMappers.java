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

package com.facebook.buck.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class ObjectMappers {

  private ObjectMappers() {

  }

  public static ObjectMapper newDefaultInstance() {
    ObjectMapper mapper = new ObjectMapper();
    // Disable automatic flush() after mapper.write() call, because it is usually unnecessary,
    // and it makes BufferedOutputStreams to be useless
    mapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    // Add support for serializing Guava collections.
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new Jdk8Module());
    return mapper;
  }
}
