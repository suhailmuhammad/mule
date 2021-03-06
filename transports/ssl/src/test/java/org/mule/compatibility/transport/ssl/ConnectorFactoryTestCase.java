/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import org.junit.Test;

public class ConnectorFactoryTestCase extends AbstractMuleContextEndpointTestCase {

  @Test
  public void testCreate() throws Exception {
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("ssl://localhost:7877");
    assertNotNull(endpoint);
    assertNotNull(endpoint.getConnector());
    assertEquals("ssl://localhost:7877", endpoint.getEndpointURI().getAddress());
  }

}
