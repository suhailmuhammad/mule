/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.runtime.api.message.Attributes;

import java.util.HashMap;
import java.util.Map;

//TODO Remove once MULE-10947 gets done.
public class PolicyHttpRequestAttributes implements Attributes
{

  private Map<String, String> headers = new HashMap<>();
  private Map<String, String> queryParams = new HashMap<>();

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setQueryParams(Map<String, String> queryParams) {
    this.queryParams = queryParams;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }
}
