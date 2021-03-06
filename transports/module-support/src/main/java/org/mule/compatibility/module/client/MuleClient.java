/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_MAP;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import static org.mule.runtime.core.security.MuleCredentials.createHeader;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.ReceiveException;
import org.mule.compatibility.core.config.builders.TransportsConfigurationBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.FutureMessageResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.client.DefaultLocalMuleClient.MuleClientFlowConstruct;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.runtime.core.transformer.TransformerUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MuleClient</code> is a simple interface for Mule clients to send and receive events from a Mule Server. In most Mule
 * applications events are triggered by some external occurrence such as a message being received on a queue or a file being
 * copied to a directory. The Mule client allows the user to send and receive events programmatically through its API.
 * <p>
 * The client defines a {@link EndpointURI} which is used to determine how a message is sent of received. The url defines the
 * protocol, the endpointUri destination of the message and optionally the endpoint to use when dispatching the event. For
 * example:
 * <p>
 * <code>vm://my.object</code> dispatches to a <code>my.object</code> destination using the VM endpoint. There needs to be a
 * global VM endpoint registered for the message to be sent.
 * <p>
 * <code>jms://jmsProvider/orders.topic</code> dispatches a JMS message via the globally registered jmsProvider over a topic
 * destination called <code>orders.topic</code>.
 * <p>
 * <code>jms://orders.topic</code> is equivalent to the above except that the endpoint is determined by the protocol, so the first
 * JMS endpoint is used.
 * <p>
 * Note that there must be a configured MuleManager for this client to work. It will use the one available using
 * <code>muleContext</code>
 *
 * @see org.mule.compatibility.core.endpoint.MuleEndpointURI
 */
public class MuleClient implements Disposable {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(MuleClient.class);

  private static final int TIMEOUT_NOT_SET_VALUE = Integer.MIN_VALUE;

  /**
   * The local MuleContext instance.
   */
  private MuleContext muleContext;
  private MuleClientFlowConstruct flowConstruct;

  private MuleCredentials user;

  private DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();

  private ConcurrentMap<String, InboundEndpoint> inboundEndpointCache = new ConcurrentHashMap<>();
  private ConcurrentMap<String, OutboundEndpoint> outboundEndpointCache = new ConcurrentHashMap<>();


  /**
   * Creates a Mule client that will use the default serverEndpoint when connecting to a remote server instance.
   *
   * @throws MuleException
   */
  protected MuleClient() throws MuleException {
    this(true);
  }

  public MuleClient(boolean startContext) throws MuleException {
    init(startContext);
  }

  public MuleClient(MuleContext context) throws MuleException {
    this.muleContext = context;
    init(false);
  }

  /**
   * Configures a Mule client instance using the the default {@link SpringXmlConfigurationBuilder} to parse
   * <code>configResources</code>.
   *
   * @param configResources a config resource location to configure this client with
   * @throws ConfigurationException if there is a {@link MuleContext} instance already running in this JVM or if the builder fails
   *         to configure the Manager
   */
  public MuleClient(String configResources) throws MuleException {
    this(configResources, new SpringXmlConfigurationBuilder(configResources));
  }

  /**
   * Configures a new Mule client and either uses an existing Manager running in this JVM or creates a new empty
   * {@link MuleContext}
   *
   * @param user the username to use when connecting to a remote server instance
   * @param password the password for the user
   * @throws MuleException
   */
  public MuleClient(String user, String password) throws MuleException {
    init(/* startManager */true);
    this.user = new MuleCredentials(user, password.toCharArray());
  }

  /**
   * Configures a Mule client instance
   *
   * @param configResources a config resource location to configure this client with
   * @param builder the configuration builder to use
   * @throws ConfigurationException is there is a {@link MuleContext} instance already running in this JVM or if the builder fails
   *         to configure the Manager
   * @throws InitialisationException
   */
  public MuleClient(String configResources, ConfigurationBuilder builder) throws ConfigurationException, InitialisationException {
    if (builder == null) {
      logger.info("Builder passed in was null, using default builder: " + SpringXmlConfigurationBuilder.class.getName());
      builder = new SpringXmlConfigurationBuilder(configResources);
    }
    logger.info("Initializing Mule...");
    muleContext = muleContextFactory.createMuleContext(builder);
  }

  /**
   * Configures a Mule client instance
   *
   * @param configResources a config resource location to configure this client with
   * @param builder the configuration builder to use
   * @param user the username to use when connecting to a remote server instance
   * @param password the password for the user
   * @throws ConfigurationException is there is a {@link MuleContext} instance already running in this JVM or if the builder fails
   *         to configure the Manager
   * @throws InitialisationException
   */
  public MuleClient(String configResources, ConfigurationBuilder builder, String user, String password)
      throws ConfigurationException, InitialisationException {
    this(configResources, builder);
    this.user = new MuleCredentials(user, password.toCharArray());
  }

  /**
   * Initialises a default {@link MuleContext} for use by the client.
   *
   * @param startManager start the Mule context if it has not yet been initialised
   * @throws MuleException
   */
  private void init(boolean startManager) throws MuleException {
    if (muleContext == null) {
      logger.info("No existing ManagementContext found, creating a new Mule instance");

      MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
      DefaultMuleConfiguration config = new DefaultMuleConfiguration();
      config.setClientMode(true);
      contextBuilder.setMuleConfiguration(config);
      muleContext =
          muleContextFactory.createMuleContext(asList(new TransportsConfigurationBuilder(), new AbstractConfigurationBuilder() {

            @Override
            public void doConfigure(MuleContext muleContext) throws Exception {
              MuleRegistry registry = muleContext.getRegistry();
              final StandaloneClientSchedulerService schedulerService = new StandaloneClientSchedulerService();
              schedulerService.start();
              registry.registerObject(schedulerService.getName(), schedulerService);
            }
          }), contextBuilder);
    } else {
      logger.info("Using existing MuleContext: " + muleContext);
    }

    if (!muleContext.isStarted() && startManager == true) {
      logger.info("Starting Mule...");
      muleContext.start();
    }
    this.flowConstruct = new MuleClientFlowConstruct(muleContext);
  }

  /**
   * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL determines where to dispatch the event to.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of JMS you could set the JMSReplyTo
   *        property in these properties.
   * @throws org.mule.api.MuleException
   */
  public void dispatch(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException {
    if (messageProperties == null) {
      messageProperties = EMPTY_MAP;
    }
    dispatch(url, InternalMessage.builder().payload(payload).outboundProperties(messageProperties).build());
  }

  /**
   * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL determines where to dispatch the event to.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the message to send
   * @throws org.mule.api.MuleException
   */
  public void dispatch(String url, InternalMessage message) throws MuleException {
    OutboundEndpoint endpoint = getOutboundEndpoint(url, ONE_WAY, null);
    Event event = getEvent(message, ONE_WAY);
    endpoint.process(event);
  }

  /**
   * Sends an event request to a URL, making the result of the event trigger available as a Future result that can be accessed
   * later by client code.
   *
   * @param url the url to make a request on
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. as null
   * @return the result message if any of the invocation
   * @throws org.mule.api.MuleException if the dispatch fails or the components or transfromers cannot be found
   */
  public FutureMessageResult sendAsync(String url, Object payload, Map<String, Serializable> messageProperties)
      throws MuleException {
    return sendAsync(url, payload, messageProperties, 0);
  }

  /**
   * Sends an event request to a URL, making the result of the event trigger available as a Future result that can be accessed
   * later by client code.
   *
   * @param url the URL to make a request on
   * @param message the message to send
   * @return the result message if any of the invocation
   * @throws org.mule.api.MuleException if the dispatch fails or the components or transfromers cannot be found
   */
  public FutureMessageResult sendAsync(final String url, final InternalMessage message) throws MuleException {
    return sendAsync(url, message, TIMEOUT_NOT_SET_VALUE);
  }

  /**
   * Sends an event request to a URL, making the result of the event trigger available as a Future result that can be accessed
   * later by client code.
   *
   * @param url the url to make a request on
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. as null
   * @param timeout how long to block in milliseconds waiting for a result
   * @return the result message if any of the invocation
   * @throws org.mule.api.MuleException if the dispatch fails or the components or transfromers cannot be found
   */
  public FutureMessageResult sendAsync(final String url, final Object payload, final Map<String, Serializable> messageProperties,
                                       final int timeout)
      throws MuleException {
    Map<String, Serializable> outboundProperties = messageProperties;
    if (messageProperties == null) {
      outboundProperties = EMPTY_MAP;
    }
    return sendAsync(url, InternalMessage.builder().payload(payload).outboundProperties(outboundProperties).build(), timeout);
  }

  /**
   * Sends an event request to a URL, making the result of the event trigger available as a Future result that can be accessed
   * later by client code.
   *
   * @param url the url to make a request on
   * @param message the message to send
   * @param timeout how long to block in milliseconds waiting for a result
   * @return the result message if any of the invocation
   * @throws org.mule.api.MuleException if the dispatch fails or the components or transfromers cannot be found
   */
  public FutureMessageResult sendAsync(final String url, final InternalMessage message, final int timeout) throws MuleException {
    Callable<Object> call = () -> send(url, message, timeout);

    FutureMessageResult result = new FutureMessageResult(call, muleContext);

    if (muleContext.getWorkManager() != null) {
      result.setExecutor(muleContext.getWorkManager());
    }

    result.execute();
    return result;
  }

  /**
   * Sends an event synchronously to a endpointUri via a Mule server and a resulting message is returned.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of Jms you could set the JMSReplyTo
   *        property in these properties.
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  public InternalMessage send(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException {
    return send(url, payload, messageProperties, TIMEOUT_NOT_SET_VALUE);
  }

  /**
   * Sends an event synchronously to a endpointUri via a Mule server and a resulting message is returned.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message the Message for the event
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  public InternalMessage send(String url, InternalMessage message) throws MuleException {
    return send(url, message, TIMEOUT_NOT_SET_VALUE);
  }

  /**
   * Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of Jms you could set the JMSReplyTo
   *        property in these properties.
   * @param timeout The time in milliseconds the the call should block waiting for a response
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  public InternalMessage send(String url, Object payload, Map<String, Serializable> messageProperties, int timeout)
      throws MuleException {
    if (messageProperties == null) {
      messageProperties = new HashMap<>();
    }
    if (messageProperties.get(MULE_REMOTE_SYNC_PROPERTY) == null) {
      // clone the map in case a call used an unmodifiable version
      messageProperties = new HashMap<>(messageProperties);
      messageProperties.put(MULE_REMOTE_SYNC_PROPERTY, "true");
    }
    InternalMessage message = InternalMessage.builder().payload(payload).outboundProperties(messageProperties).build();
    return send(url, message, timeout);
  }

  /**
   * Sends an event synchronously to a endpointUri via a mule server and a resulting message is returned.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param message The message to send
   * @param timeout The time in milliseconds the the call should block waiting for a response
   * @return A return message, this could be <code>null</code> if the the components invoked explicitly sets a return as
   *         <code>null</code>.
   * @throws org.mule.api.MuleException
   */
  public InternalMessage send(String url, InternalMessage message, int timeout) throws MuleException {
    OutboundEndpoint endpoint = getOutboundEndpoint(url, REQUEST_RESPONSE, timeout);

    Event event = getEvent(message, REQUEST_RESPONSE);

    Event response = endpoint.process(event);
    if (response != null) {
      return response.getMessage();
    } else {
      return InternalMessage.builder().nullPayload().build();
    }
  }

  /**
   * Will receive an event from an endpointUri determined by the URL.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param timeout how long to block waiting to receive the event, if set to 0 the receive will not wait at all and if set to -1
   *        the receive will wait forever
   * @return the message received or <code>null</code> if no message was received
   * @throws org.mule.api.MuleException
   */
  public InternalMessage request(String url, long timeout) throws MuleException {
    InboundEndpoint endpoint = getInboundEndpoint(url);
    try {
      return endpoint.request(timeout);
    } catch (Exception e) {
      throw new ReceiveException(endpoint, timeout, e);
    }
  }

  /**
   * Will receive an event from an endpointUri determined by the URL
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param transformers A comma separated list of transformers used to apply to the result message
   * @param timeout how long to block waiting to receive the event, if set to 0 the receive will not wait at all and if set to -1
   *        the receive will wait forever
   * @return the message received or <code>null</code> if no message was received
   * @throws org.mule.api.MuleException
   */
  public InternalMessage request(String url, String transformers, long timeout) throws MuleException {
    return request(url, TransformerUtils.getTransformers(transformers, muleContext), timeout);
  }

  /**
   * Will receive an event from an endpointUri determined by the URL
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param transformers Transformers used to modify the result message
   * @param timeout how long to block waiting to receive the event, if set to 0 the receive will not wait at all and if set to -1
   *        the receive will wait forever
   * @return the message received or <code>null</code> if no message was received
   * @throws org.mule.api.MuleException
   */
  public InternalMessage request(String url, List<?> transformers, long timeout) throws MuleException {
    return request(url, timeout);
  }

  protected Event getEvent(InternalMessage message, MessageExchangePattern exchangePattern) throws MuleException {
    if (user != null) {
      message = InternalMessage.builder(message)
          .addOutboundProperty(MULE_USER_PROPERTY, createHeader(user.getUsername(), user.getPassword())).build();
    }
    MuleClientFlowConstruct flowConstruct = new MuleClientFlowConstruct(muleContext);
    return Event.builder(DefaultEventContext.create(flowConstruct, "MuleClient")).message(message)
        .exchangePattern(exchangePattern).flow(flowConstruct).build();
  }

  protected InboundEndpoint getInboundEndpoint(String uri) throws MuleException {
    // There was a potential leak here between get() and putIfAbsent(). This
    // would cause the endpoint that was created to be used rather an endpoint
    // with the same key that has been created and put in the cache by another
    // thread. To avoid this we test for the result of putIfAbsent result and if
    // it is non-null then an endpoint was created and added concurrently and we
    // return this instance instead.
    InboundEndpoint endpoint = inboundEndpointCache.get(uri);
    if (endpoint == null) {
      endpoint = getEndpointFactory().getInboundEndpoint(uri);
      InboundEndpoint concurrentlyAddedEndpoint = inboundEndpointCache.putIfAbsent(uri, endpoint);
      if (concurrentlyAddedEndpoint != null) {
        return concurrentlyAddedEndpoint;
      }
    }
    return endpoint;
  }

  protected OutboundEndpoint getOutboundEndpoint(String uri, MessageExchangePattern exchangePattern, Integer responseTimeout)
      throws MuleException {
    // There was a potential leak here between get() and putIfAbsent(). This
    // would cause the endpoint that was created to be used rather an endpoint
    // with the same key that has been created and put in the cache by another
    // thread. To avoid this we test for the result of putIfAbsent result and if
    // it is non-null then an endpoint was created and added concurrently and we
    // return this instance instead.
    String key = String.format("%1s:%2s:%3s", uri, exchangePattern, responseTimeout);
    OutboundEndpoint endpoint = outboundEndpointCache.get(key);
    if (endpoint == null) {
      EndpointBuilder endpointBuilder = getEndpointFactory().getEndpointBuilder(uri);
      endpointBuilder.setExchangePattern(exchangePattern);
      if (responseTimeout != null && responseTimeout > 0) {
        endpointBuilder.setResponseTimeout(responseTimeout.intValue());
      }
      endpoint = getEndpointFactory().getOutboundEndpoint(endpointBuilder);
      endpoint.setFlowConstruct(flowConstruct);
      OutboundEndpoint concurrentlyAddedEndpoint = outboundEndpointCache.putIfAbsent(key, endpoint);
      if (concurrentlyAddedEndpoint != null) {
        return concurrentlyAddedEndpoint;
      }
    }
    return endpoint;
  }

  /**
   * Sends an event synchronously to a endpointUri via a Mule server without waiting for the result.
   *
   * @param url the Mule URL used to determine the destination and transport of the message
   * @param payload the object that is the payload of the event
   * @param messageProperties any properties to be associated with the payload. In the case of Jms you could set the JMSReplyTo
   *        property in these properties.
   * @throws org.mule.api.MuleException
   */
  public void sendNoReceive(String url, Object payload, Map<String, Serializable> messageProperties) throws MuleException {
    if (messageProperties == null) {
      messageProperties = new HashMap<>();
    }
    messageProperties.put(MULE_REMOTE_SYNC_PROPERTY, "false");
    if (messageProperties == null) {
      messageProperties = EMPTY_MAP;
    }
    InternalMessage message = InternalMessage.builder().payload(payload).outboundProperties(messageProperties).build();

    OutboundEndpoint endpoint = getOutboundEndpoint(url, REQUEST_RESPONSE, null);
    Event event = getEvent(message, REQUEST_RESPONSE);
    endpoint.process(event);
  }

  /**
   * The overriding method may want to return a custom {@link MuleContext} here
   *
   * @return the MuleContext to use
   */
  public MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * Will dispose the MuleManager instance <b>if</b> a new instance was created for this client. Otherwise this method only cleans
   * up resources no longer needed
   */
  @Override
  public void dispose() {
    // Dispose the muleContext only if the muleContext was created for this
    // client
    if (muleContext.getConfiguration().isClientMode()) {
      logger.info("Stopping Mule...");
      muleContext.dispose();
    }
  }

  public void setProperty(String key, Object value) {
    try {
      muleContext.getRegistry().registerObject(key, value);
    } catch (RegistrationException e) {
      logger.error("Cannot set property '{}'", key, e);
    }
  }

  public Object getProperty(String key) {
    return muleContext.getRegistry().lookupObject(key);
  }

  public MuleConfiguration getConfiguration() {
    return muleContext.getConfiguration();
  }

  private EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(OBJECT_MULE_ENDPOINT_FACTORY);
  }

}
