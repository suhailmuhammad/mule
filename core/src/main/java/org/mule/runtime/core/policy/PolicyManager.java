/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Manager for handling policies in Mule.
 * 
 * Implementation of this class will be used to lookup for {@code Policy}s that must be applied to {@code MessageSource}es or
 * {@code Processor}s.
 * 
 * @since 4.0
 */
public interface PolicyManager {

  /**
   * Lookups for a {@code Policy} based on the {@code ComponentIdentifier} which may be a source or an operation.
   *
   * @param componentIdentifier the identifier of the source or operation.
   * @return a policy to be applied to the component. It may be an empty optional if no policy was found.
   */
  Optional<Policy> lookupPolicy(ComponentIdentifier componentIdentifier);

  Optional<PolicyOperationParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier);

  Optional<PolicySourceParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier);

}
