/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.introspection.TypeIntrospecterDelegate;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.List;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.soap.SOAPHeader;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Headers of a web service operation.
 * <p>
 * This is the base class for both INPUT and OUTPUT Headers resolution, the {@link TypeIntrospecterDelegate} is in charge
 * to get the information to introspect the input or output soap headers from.
 *
 * @since 4.0
 */
final class HeadersElementResolver extends NodeElementResolver {

  HeadersElementResolver(TypeIntrospecterDelegate delegate) {
    super(delegate);
  }

  @Override
  public MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    WscConnection connection = getConnection(context);
    WsdlIntrospecter introspecter = connection.getWsdlIntrospecter();
    BindingOperation bindingOperation = introspecter.getBindingOperation(operationName);
    ElementExtensible bindingType = delegate.getBindingType(bindingOperation);
    List<SOAPHeader> headers = getHeaders(bindingType);
    if (!headers.isEmpty()) {
      Message message = delegate.getMessage(introspecter.getOperation(operationName));
      return buildHeaderType(context.getTypeBuilder(), connection.getTypeLoader(), headers, message);
    }
    return NULL_TYPE;
  }

  private MetadataType buildHeaderType(BaseTypeBuilder baseTypeBuilder, TypeLoader loader, List<SOAPHeader> headers,
                                       Message message)
      throws MetadataResolvingException {
    ObjectTypeBuilder typeBuilder = baseTypeBuilder.objectType();
    for (SOAPHeader header : headers) {
      ObjectFieldTypeBuilder field = typeBuilder.addField();
      String partName = header.getPart();
      field.key(partName).value(buildPartMetadataType(loader, message.getPart(partName)));
    }
    return typeBuilder.build();
  }

  private List<SOAPHeader> getHeaders(ElementExtensible bindingType) {
    List extensible = bindingType.getExtensibilityElements();
    if (extensible != null) {
      return (List<SOAPHeader>) extensible.stream().filter(e -> e instanceof SOAPHeader).collect(toList());
    }
    return emptyList();
  }
}
