/* 
 * This file is part of the jib-maven-plugin-extension (https://github.com/thought-gang/jib-maven-plugin-extension).
 * Copyright (C) 2020 Thought Gang GmbH.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.thoughtgang.maven.jib;

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FileEntry;
import com.google.cloud.tools.jib.api.buildplan.LayerObject;
import com.google.cloud.tools.jib.maven.extension.JibMavenPluginExtension;
import com.google.cloud.tools.jib.maven.extension.MavenData;
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger;
import com.google.cloud.tools.jib.plugins.extension.JibPluginExtensionException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author fhossfel
 */
public class BundlePackagingExtension implements JibMavenPluginExtension<BundlePackagingExtensionConfiguration> {

  private static final String JIB_MAVEN_PLUGIN = "com.google.cloud.tools:jib-maven-plugin";
  //private Map<PathMatcher, String> pathMatchers = new LinkedHashMap<>();
  
  
  @Override
  public Optional<Class<BundlePackagingExtensionConfiguration>> getExtraConfigType() {
    return Optional.of(BundlePackagingExtensionConfiguration.class);
  }

  @Override
  public ContainerBuildPlan extendContainerBuildPlan(
      ContainerBuildPlan buildPlan,
      Map<String, String> properties,
      Optional<BundlePackagingExtensionConfiguration> config,
      MavenData mavenData,
      ExtensionLogger logger)
      throws JibPluginExtensionException {
      
    checkConfig(mavenData, logger);

    String appRoot;

    Xpp3Dom pluginConfig = getConfig(mavenData);
    Xpp3Dom containerConfig = pluginConfig.getChild("container");
    
    if (containerConfig != null && containerConfig.getChild("appRoot") != null) {
      
      appRoot = containerConfig.getChild("appRoot").getValue();
    
    }  else {
    
      logger.log(ExtensionLogger.LogLevel.WARN, "Configuration option \"appRoot\" has not been set. Are you sure?");
      appRoot = "/app";
      
    }
    
    FileEntriesLayer.Builder layerBuilder = null;
 
    List<LayerObject> layers = new ArrayList<LayerObject>();
    List<FileEntry> fileEntries = new ArrayList<FileEntry>();

    
    for (LayerObject layer : buildPlan.getLayers()) {
        
        
        if (layer instanceof FileEntriesLayer) {
            
            
            if (layerBuilder == null)  {
            
                layerBuilder = ((FileEntriesLayer) layer).toBuilder().setName("bundles");
                
            }
            
            for (FileEntry fileEntry : ((FileEntriesLayer)layer).getEntries()) {
        
                Path fileName = Paths.get(fileEntry.getExtractionPath().toString()).getFileName();
                AbsoluteUnixPath extractionPath = AbsoluteUnixPath.get(appRoot + "/" + fileName);
                FileEntry newFileEntry = new FileEntry(fileEntry.getSourceFile(),
                                                       extractionPath,
                                                       fileEntry.getPermissions(),
                                                       fileEntry.getModificationTime(),
                                                       fileEntry.getOwnership());
                fileEntries.add(newFileEntry);

            }

        } else {
            
            layers.add(layer);
            
        }
        
    }
    
    layerBuilder.setEntries(fileEntries);
    
    //Warning: Layers might not be in the correct order! Can this cause a problem?
    layers.add(layerBuilder.build());
    
    ContainerBuildPlan.Builder containerBuildPlanBuilder = buildPlan.toBuilder().setLayers(layers);

    return containerBuildPlanBuilder.build();

  }    

  
  private void checkConfig(MavenData mavenData, ExtensionLogger logger) throws JibPluginExtensionException {
      
    if (! "bundle".equals(mavenData.getMavenProject().getPackaging())) {
       
      throw new JibPluginExtensionException(BundlePackagingExtension.class, String.format("This extension is only for Maven prjects of the type bundle. But the current project is of the type \"{}\"", mavenData.getMavenProject().getArtifact().getType()));
    
    }
    
    Xpp3Dom pluginConfig = getConfig(mavenData);
    pluginConfig.getChild("containerizingMode");
    
    if (pluginConfig.getChild("containerizingMode") == null || ! "packaged".equals(pluginConfig.getChild("containerizingMode").getValue())) {
        
        logger.log(ExtensionLogger.LogLevel.WARN, "Configuration option \"containerizingMode\" not set to \"packaged\". Continuing anyway! Hope for the best, prepare for the worst!");
        
    }
    
    
  }
       
  private Xpp3Dom getConfig(MavenData mavenData) {
      
    return (Xpp3Dom) mavenData.getMavenProject().getBuild().getPluginsAsMap().get(JIB_MAVEN_PLUGIN).getConfiguration();    
     
      
  }

}