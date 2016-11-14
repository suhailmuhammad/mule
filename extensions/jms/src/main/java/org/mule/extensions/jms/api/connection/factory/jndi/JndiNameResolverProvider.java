/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;

import static java.lang.String.format;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a {@link JndiNameResolver} or the set of properties required to
 * create one, represented by {@link JndiNameResolverProperties}
 *
 * @since 4.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class JndiNameResolverProvider {

  private static final Logger logger = LoggerFactory.getLogger(JndiNameResolverProvider.class);

  @Parameter
  @Optional
  private JndiNameResolver customJndiNameResolver;

  @Parameter
  @Optional
  private JndiNameResolverProperties nameResolverBuilder;


  public JndiNameResolver getCustomJndiNameResolver() {
    return customJndiNameResolver;
  }

  public JndiNameResolverProperties getNameResolverBuilder() {
    return nameResolverBuilder;
  }

  JndiNameResolver createDefaultJndiResolver() throws InitialisationException {
    if (logger.isDebugEnabled()) {
      logger.debug("Creating default JndiNameResolver");
      logger.debug(format("Provider Url: [%s], InitialContextFactory: [%s]",
                          nameResolverBuilder.getJndiProviderUrl(), nameResolverBuilder.getJndiInitialFactory()));
    }

    SimpleJndiNameResolver nameResolver = new SimpleJndiNameResolver();
    nameResolver.setJndiProviderUrl(nameResolverBuilder.getJndiProviderUrl());
    nameResolver.setJndiInitialFactory(nameResolverBuilder.getJndiInitialFactory());
    nameResolver.setJndiProviderProperties(nameResolverBuilder.getProviderProperties());

    return nameResolver;
  }
}
