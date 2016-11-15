/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.execution.NextOperation;

import java.io.Serializable;

public interface OperationPolicyInstance extends Serializable {

  Event process(Event eventBeforeOperation, NextOperation next) throws Exception;

  //TODO this seems wrong here
  Event getLastEvent();

}
