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

package com.facebook.buck.jvm.java;

import com.facebook.buck.rules.ArchiveMemberSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.keys.ArchiveDependencySupplier;
import com.facebook.buck.zip.Unzip;
import com.facebook.buck.util.HumanReadableException;
import com.google.common.collect.ImmutableSortedSet;

import java.io.IOException;
import java.nio.file.Path;

public class ZipArchiveDependencySupplier implements ArchiveDependencySupplier {
  private final ImmutableSortedSet<SourcePath> zipFiles;

  public ZipArchiveDependencySupplier(ImmutableSortedSet<SourcePath> zipFiles) {
    this.zipFiles = zipFiles;
  }

  @Override
  public ImmutableSortedSet<SourcePath> get() {
    return zipFiles;
  }

  @Override
  public ImmutableSortedSet<SourcePath> getArchiveMembers(SourcePathResolver resolver) {
    ImmutableSortedSet.Builder<SourcePath> builder = ImmutableSortedSet.naturalOrder();
    for (SourcePath zipSourcePath : zipFiles) {
      final Path zipAbsolutePath = resolver.getAbsolutePath(zipSourcePath);
      try {
        for (Path member : Unzip.getZipMembers(zipAbsolutePath)) {
          builder.add(new ArchiveMemberSourcePath(zipSourcePath, member));
        }
      } catch (IOException e) {
        throw new HumanReadableException(e, "Failed to read archive: " + zipAbsolutePath);
      }
    }
    return builder.build();
  }
}
