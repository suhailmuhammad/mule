/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.pool;

import static java.lang.Thread.currentThread;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.config.PreferredObjectSelector;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.registry.SpiServiceRegistry;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Uses a standard JDK's mechanism to locate implementations.
 */
public abstract class ThreadPoolFactory implements MuleContextAware {

  protected MuleContext muleContext;

  /**
   * @return a discovered
   */
  public static ThreadPoolFactory newInstance() {
    final Iterable<ThreadPoolFactory> threadPoolFactoryServices =
        new SpiServiceRegistry().lookupProviders(ThreadPoolFactory.class, currentThread().getContextClassLoader());

    PreferredObjectSelector<ThreadPoolFactory> selector = new PreferredObjectSelector<>();
    ThreadPoolFactory threadPoolFactory = selector.select(threadPoolFactoryServices.iterator());

    if (threadPoolFactory == null) {
      throw new MuleRuntimeException(I18nMessageFactory
          .createStaticMessage("Couldn't find config via SPI mechanism. Corrupted Mule core jar?"));
    }

    return threadPoolFactory;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public abstract ThreadPoolExecutor createPool(String name, ThreadingProfile tp);

  /**
   * By limitations of java's {@link ScheduledThreadPoolExecutor}, {@link ThreadingProfile#getMaxThreadsActive()} will be ignored
   * and a fixed pool with {@link ThreadingProfile#getMaxThreadsIdle()} will be created.
   */
  public abstract ScheduledThreadPoolExecutor createScheduledPool(String name, ThreadingProfile tp);
}
