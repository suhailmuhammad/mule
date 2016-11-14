/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.provider;

import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_1_0_2b;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.extensions.jms.api.connection.factory.jndi.JndiConnectionFactory;
import org.mule.extensions.jms.internal.support.Jms102bSupport;
import org.mule.extensions.jms.internal.support.Jms11Support;
import org.mule.extensions.jms.internal.support.Jms20Support;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.jms.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * //TODO
 */
@Alias("generic")
public class GenericConnectionProvider extends BaseConnectionProvider {

  private static final Logger logger = LoggerFactory.getLogger(BaseConnectionProvider.class);

  @Parameter
  @Expression(NOT_SUPPORTED)
  private ConnectionFactory connectionFactory;


  @Override
  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  /**
   * A factory method to create various JmsSupport class versions.
   */
  protected void createJmsSupport() {
    if (!(connectionFactory instanceof JndiConnectionFactory)) {
      super.createJmsSupport();
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Creating JMSSupport using a Jndi discovered Connection Factory");
    }
    JndiConnectionFactory jndiConnectionFactory = (JndiConnectionFactory) this.connectionFactory;

    LookupJndiDestination lookupJndiDestination = jndiConnectionFactory.getLookupDestination();

    JmsSpecification specification = getConnectionParameters().getSpecification();
    if (JMS_1_0_2b.equals(specification)) {
      setJmsSupport(new Jms102bSupport(lookupJndiDestination, jndiConnectionFactory::getJndiDestination));
    } else if (JMS_2_0.equals(specification)) {
      setJmsSupport(new Jms20Support(lookupJndiDestination, jndiConnectionFactory::getJndiDestination));
    } else {
      setJmsSupport(new Jms11Support(lookupJndiDestination, jndiConnectionFactory::getJndiDestination));
    }

  }

}
