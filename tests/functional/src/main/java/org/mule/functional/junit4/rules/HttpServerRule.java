/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.rules;

import org.mule.functional.util.http.SimpleHttpServer;
import org.mule.tck.junit4.rule.FreePortFinder;

import org.junit.rules.ExternalResource;

public class HttpServerRule extends ExternalResource {

  private final String portSystemPropertyKey;
  private SimpleHttpServer simpleHttpServer;


  public HttpServerRule(String portSystemPropertyKey) {
    this.portSystemPropertyKey = portSystemPropertyKey;
  }

  @Override
  protected void before() throws Throwable {
    Integer port = new FreePortFinder(0, 7000).find();
    simpleHttpServer = SimpleHttpServer.createServer(port).start();
    System.setProperty(portSystemPropertyKey, String.valueOf(port));
  }

  @Override
  protected void after() {
    System.clearProperty(portSystemPropertyKey);
    simpleHttpServer.stop();
  }

  public SimpleHttpServer getSimpleHttpServer() {
    return simpleHttpServer;
  }
}
