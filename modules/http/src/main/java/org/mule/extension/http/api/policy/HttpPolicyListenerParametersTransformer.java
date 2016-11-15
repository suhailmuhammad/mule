/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static org.mule.runtime.dsl.api.component.ComponentIdentifier.parseComponentIdentifier;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.listener.builder.HttpListenerErrorResponseBuilder;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.extension.http.api.listener.builder.HttpListenerSuccessResponseBuilder;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class HttpPolicyListenerParametersTransformer implements PolicySourceParametersTransformer
{

  @Override
  public boolean supports(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.equals(parseComponentIdentifier("http:listener"));
  }

  @Override
  public Message fromParametersToMessage(Map<String, Object> parameters) {
    HttpListenerResponseBuilder responseBuilder = (HttpListenerResponseBuilder) parameters.get("responseBuilder");
    if (responseBuilder == null) {
      responseBuilder = (HttpListenerResponseBuilder) parameters.get("errorResponseBuilder");
    }
    ParameterMap headers = new ParameterMap(responseBuilder.getHeaders());
    return Message.builder().payload(responseBuilder.getBody())
        .attributes(new HttpResponseAttributes(responseBuilder.getStatusCode(), responseBuilder.getReasonPhrase(), headers))
        .build();
  }

  @Override
  public Map<String, Object> fromMessageToSuccessResponseParameters(Message message)
  {
    if (message.getAttributes() instanceof HttpResponseAttributes) {
      HttpResponseAttributes httpResponseAttributes = (HttpResponseAttributes) message.getAttributes();
      HttpListenerSuccessResponseBuilder httpListenerSuccessResponseBuilder = new HttpListenerSuccessResponseBuilder();
      httpListenerSuccessResponseBuilder.setBody(message.getPayload().getValue());
      // TODO change this code to work with collection
      for (String headerName : httpResponseAttributes.getHeaders().keySet()) {
        httpListenerSuccessResponseBuilder.getHeaders().put(headerName, httpResponseAttributes.getHeaders().get(headerName));
      }

      // TODO see media type
      httpListenerSuccessResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerSuccessResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      //TODO fix putting both
      return ImmutableMap.<String, Object>builder().put("responseBuilder", httpListenerSuccessResponseBuilder).put("errorResponseBuilder", httpListenerSuccessResponseBuilder).build();
    } else if (message.getAttributes() instanceof PolicyHttpResponseAttributes) {
      PolicyHttpResponseAttributes httpResponseAttributes = (PolicyHttpResponseAttributes) message.getAttributes();
      HttpListenerSuccessResponseBuilder httpListenerSuccessResponseBuilder = new HttpListenerSuccessResponseBuilder();
      httpListenerSuccessResponseBuilder.setBody(message.getPayload().getValue());
      // TODO change this code to work with collection
      for (String headerName : httpResponseAttributes.getHeaders().keySet()) {
        httpListenerSuccessResponseBuilder.getHeaders().put(headerName, httpResponseAttributes.getHeaders().get(headerName));
      }

      // TODO see media type
      httpListenerSuccessResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerSuccessResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return ImmutableMap.<String, Object>builder().put("responseBuilder", httpListenerSuccessResponseBuilder).put("errorResponseBuilder", httpListenerSuccessResponseBuilder).build();
    } else {
      // TODO fix
      throw new RuntimeException("");
    }
  }

  @Override
  public Map<String, Object> fromMessageToErrorResponseParameters(Message message)
  {
    if (message.getAttributes() instanceof HttpResponseAttributes) {
      HttpResponseAttributes httpResponseAttributes = (HttpResponseAttributes) message.getAttributes();
      HttpListenerErrorResponseBuilder httpListenerErrorResponseBuilder = new HttpListenerErrorResponseBuilder();
      httpListenerErrorResponseBuilder.setBody(message.getPayload().getValue());

      // TODO change this code to work with collection
      for (String headerName : httpResponseAttributes.getHeaders().keySet()) {
        httpListenerErrorResponseBuilder.getHeaders().put(headerName, httpResponseAttributes.getHeaders().get(headerName));
      }

      // TODO see media type
      httpListenerErrorResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerErrorResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      //TODO fix putting both
      return ImmutableMap.<String, Object>builder().put("responseBuilder", httpListenerErrorResponseBuilder).put("errorResponseBuilder", httpListenerErrorResponseBuilder).build();
    } else if (message.getAttributes() instanceof PolicyHttpResponseAttributes) {
      PolicyHttpResponseAttributes httpResponseAttributes = (PolicyHttpResponseAttributes) message.getAttributes();
      HttpListenerErrorResponseBuilder httpListenerSuccessResponseBuilder = new HttpListenerErrorResponseBuilder();
      httpListenerSuccessResponseBuilder.setBody(message.getPayload().getValue());
      // TODO change this code to work with collection
      for (String headerName : httpResponseAttributes.getHeaders().keySet()) {
        httpListenerSuccessResponseBuilder.getHeaders().put(headerName, httpResponseAttributes.getHeaders().get(headerName));
      }

      // TODO see media type
      httpListenerSuccessResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerSuccessResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return ImmutableMap.<String, Object>builder().put("responseBuilder", httpListenerSuccessResponseBuilder).put("errorResponseBuilder", httpListenerSuccessResponseBuilder).build();
    } else {
      // TODO fix
      throw new RuntimeException("");
    }
  }

}
