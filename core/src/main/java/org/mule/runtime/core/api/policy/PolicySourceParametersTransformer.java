/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;

public interface PolicySourceParametersTransformer
{

    boolean supports(ComponentIdentifier componentIdentifier);

    /**
     * Transformers a set of parameters to a message that can be route through a pipeline
     * @param parameters
     * @return
     */
    Message fromParametersToMessage(Map<String, Object> parameters);


    Map<String, Object> fromMessageToSuccessResponseParameters(Message message);

    Map<String, Object> fromMessageToErrorResponseParameters(Message message);

}
