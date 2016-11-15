/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

public interface Policy {

  OperationPolicyInstance createSourcePolicyInstance(String id, ComponentIdentifier sourceIdentifier);

  OperationPolicyInstance createOperationPolicyInstance(String id, ComponentIdentifier operationIdentifier);

}
