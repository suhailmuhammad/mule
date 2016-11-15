/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.application;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.describer.MuleArtifactDescriber;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.persistence.descriptor.MuleArtifactDescriberJsonSerializer;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConfigurationBuilder} that registers a {@link ExtensionManager}
 *
 * @since 4.0
 */
public class ApplicationExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  public static final String META_INF_FOLDER = "META-INF";
  public static final String MULE_PLUGIN_JSON = "mule-plugin.json";
  private static Logger LOGGER = LoggerFactory.getLogger(ApplicationExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
  private final List<ArtifactPlugin> artifactPlugins;

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins) {
    this(artifactPlugins, new DefaultExtensionManagerAdapterFactory());
  }

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionManagerAdapterFactory extensionManagerAdapterFactory) {
    this.artifactPlugins = artifactPlugins;
    this.extensionManagerAdapterFactory = extensionManagerAdapterFactory;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

    for (ArtifactPlugin artifactPlugin : artifactPlugins) {
      URL manifestUrl =
          artifactPlugin.getArtifactClassLoader().findResource(META_INF_FOLDER + "/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Discovered extension " + artifactPlugin.getArtifactName());
        }
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        extensionManager.registerExtension(extensionManifest, artifactPlugin.getArtifactClassLoader().getClassLoader());
      } else {
        discoverExtensionThroughJsonDescriber(artifactPlugin, extensionManager);

      }
    }
  }

  /**
   * Looks for an extension using the {@link #MULE_PLUGIN_JSON} file, where if available it will parse it calling the
   * {@link XmlBasedDescriber} describer.
   *
   * TODO(fernandezlautaro): MULE-10876 all this code will be merged within the {@link ExtensionManagerAdapterFactory} where the {@link ExtensionManifest} will be dropped and the correct {@link Describer} will be picked up (either Java or XML based)
   *
   * @param artifactPlugin to introspect for the {@link #MULE_PLUGIN_JSON} and further resources from its {@link ClassLoader}
   * @param extensionManager object to store the generated {@link ExtensionModel} if exists
   */
  private void discoverExtensionThroughJsonDescriber(ArtifactPlugin artifactPlugin, ExtensionManagerAdapter extensionManager) {
    File rootPluginFolder = artifactPlugin.getDescriptor().getRootFolder();
    File jsonFile = new File(rootPluginFolder, META_INF_FOLDER + separator + MULE_PLUGIN_JSON);

    if (!jsonFile.exists()) {
      return;
    }
    MuleArtifactDescriber muleArtifactDescriber = getMuleArtifactDescriber(jsonFile);

    if (!muleArtifactDescriber.getExtensionModelDescriptor().getId().equals(XmlBasedDescriber.DESCRIBER_ID)) {
      // TODO: MULE-10876 work this ID with SPI, JAVA vs XML implementation should be discovered and validated against them
      throw new IllegalArgumentException(format("The id '%s' does not match with the describers available to generate an ExtensionModel",
                                                muleArtifactDescriber.getExtensionModelDescriptor().getId()));
    }
    // TODO: MULE-10876 the code below where it casts to a String must be moved to each concrete Describer implementation (it should receive the Map<String,Object> held by the muleArtifactDescriber.getExtensionModelDescriptor().getAttributes() messages
    String modulePath = (String) muleArtifactDescriber.getExtensionModelDescriptor().getAttributes().get("resource-xml");

    ClassLoader pluginClassLoader = artifactPlugin.getArtifactClassLoader().getClassLoader();
    DescribingContext context = new DefaultDescribingContext(pluginClassLoader);
    ExtensionFactory defaultExtensionFactory = new DefaultExtensionFactory(emptyList(), emptyList());
    XmlBasedDescriber describer = new XmlBasedDescriber(modulePath);
    ExtensionModel extensionModel =
        withContextClassLoader(pluginClassLoader, () -> defaultExtensionFactory.createFrom(describer.describe(context), context));

    extensionManager.registerExtension(extensionModel);
  }

  private MuleArtifactDescriber getMuleArtifactDescriber(File jsonFile) {
    try (InputStream stream = new FileInputStream(jsonFile)) {
      return new MuleArtifactDescriberJsonSerializer().deserialize(IOUtils.toString(stream));
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read extension describer on plugin "
          + jsonFile.getAbsolutePath()),
                                     e);
    }
  }

  private ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) throws InitialisationException {
    try {
      return extensionManagerAdapterFactory.createExtensionManager(muleContext);
    } catch (Exception e) {
      throw new InitialisationException(e, muleContext);
    }
  }
}
