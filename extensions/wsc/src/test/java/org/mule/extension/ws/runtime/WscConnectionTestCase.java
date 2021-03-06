/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static java.lang.Thread.currentThread;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.assertSoapResponse;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Message;

import java.net.URL;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Connection")
public class WscConnectionTestCase extends AbstractSoapServiceTestCase {

  private static final String SAME_INSTANCE_FLOW = "operationShareInstance";
  private static final String LOCAL_WSDL_FLOW = "withLocalWsdlConnection";

  @Override
  protected String getConfigFile() {
    return "config/connection.xml";
  }

  @Test
  @Description("Consumes 2 operations sharing the same connection instance")
  public void sameConnection() throws Exception {
    Message msg = flowRunner(SAME_INSTANCE_FLOW).withVariable("req", getRequestResource(ECHO)).run().getMessage();
    String out = (String) msg.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }

  @Test
  @Description("Consumes an operation using a connection that uses a local .wsdl file")
  public void localWsdlConnection() throws Exception {
    URL wsdl = currentThread().getContextClassLoader().getResource("wsdl/simple-service.wsdl");
    Message msg = flowRunner(LOCAL_WSDL_FLOW)
        .withVariable("req", getRequestResource(ECHO))
        .withVariable("wsdl", wsdl.getPath())
        .run().getMessage();
    String out = (String) msg.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }
}
