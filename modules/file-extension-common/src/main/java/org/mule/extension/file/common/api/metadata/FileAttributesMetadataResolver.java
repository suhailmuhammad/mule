/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.metadata;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileSystem;

import java.nio.file.FileSystemNotFoundException;
import java.util.Optional;

/**
 * Resolves the {@link MetadataType} of the {@link FileAttributes} for the specific connection that is being used.
 *
 * @since 1.0
 */
public final class FileAttributesMetadataResolver implements AttributesTypeResolver {

  private ClassTypeLoader typeLoader;

  public FileAttributesMetadataResolver() {
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    Optional<FileSystem> connection = context.getConnection();
    FileSystem fileSystem =
        connection.orElseThrow(() -> new FileSystemNotFoundException("Could not found file system to retrieve metadata from"));
    return typeLoader.load(fileSystem.getAttributesType());
  }

  @Override
  public String getCategoryName() {
    return "FileAttributesCategory";
  }
}
