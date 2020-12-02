# jib-maven-plugin-extension
A plugin extension for [Google's Java Image Builder (JIB)](https://github.com/GoogleContainerTools/jib) Maven plugin that let's package OSGI bundles.

## Purpose
Google JIB is a great tool but unfortunately due to their ["opinionated" approach](https://github.com/GoogleContainerTools/jib/issues/2562) it only support the packaging type "war" and "gwt-app". It is not possible to simply take a OSGI bundle jar (and it's dependencies) to put in some directory.

This extension will add support for the packaging type "bundle" and build a Docker image in the most simple way: The final image will only have a single layer with the bundle and its dependenceis.

## How to use it

Below you will find a complete example of the configuration:

            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>2.6.0</version>
                <dependencies>
                    <dependency>
                        <groupId>de.thoughtgang.maven.jib</groupId>
                        <artifactId>jib-maven-plugin-extension</artifactId>
                        <version>0.1-SNAPSHOT</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <from>
                        <image>mkroli/servicemix:latest</image>
                    </from>
                    <to>
                        <image>thoughtgang/test-jib</image>
                        <tags>
                            <tag>latest</tag>
                        </tags>
                    </to>
                    <container>
                        <appRoot>/delayed_deploy</appRoot>
                        <entrypoint>INHERIT</entrypoint>
                    </container>                    
                    <containerizingMode>packaged</containerizingMode>
                    <pluginExtensions>
                        <pluginExtension>
                            <implementation>de.thoughtgang.maven.jib.BundlePackagingExtension</implementation>
                        </pluginExtension>
                    </pluginExtensions>
                </configuration>		
            </plugin> 
            
The extension itself does not require or allow any configuration. But there are some requirements:

- The packaging type *must* be "bundle". If you need support for any other packaging type it should be easy to modify the checkConfig() method.
- The "containerizingMode" should be "packaged". You will receive a warning if this is not the case.
- The option "appRoot" should point to the appropriate directory. The default value of "/app" is probably not very usefull for OSGI bundles.
