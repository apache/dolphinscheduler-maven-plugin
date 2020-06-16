/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * create the spi services file
 */
@Mojo(name = "generate-dolphin-service-descriptor",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class DolphinDescriptorGenerator extends AbstractMojo {
    private static final String LS_ALIAS = System.getProperty("line.separator");

    @Parameter(defaultValue = "org.apache.dolphinscheduler.spi.DolphinSchedulerPlugin")
    private String pluginClassName;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/services")
    private File servicesDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute()
            throws MojoExecutionException
    {
        File spiServicesFile = new File(servicesDirectory, pluginClassName);

        // If users have already provided their own service file then we will not overwrite it
        if (spiServicesFile.exists()) {
            return;
        }

        if (!spiServicesFile.getParentFile().exists()) {
            File file = spiServicesFile.getParentFile();
            file.mkdirs();
            if (!file.isDirectory()) {
                throw new MojoExecutionException(String.format("%n%nFailed to create directory: %s", file));
            }
        }

        List<Class<?>> pluginImplClasses;
        try {
            URLClassLoader loader = createCLFromCompileTimeDependencies();
            pluginImplClasses = findPluginImplClasses(loader);
        }
        catch (Exception e) {
            throw new MojoExecutionException(String.format("%n%nError for find the classes that implements %s.", pluginClassName), e);
        }

        if (pluginImplClasses.isEmpty()) {
            throw new MojoExecutionException(String.format("%n%nNot find classes implements %s, You must have at least one class that implements %s.", pluginClassName, pluginClassName));
        }

        if (pluginImplClasses.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (Class<?> pluginClass : pluginImplClasses) {
                sb.append(pluginClass.getName()).append(LS_ALIAS);
            }
            throw new MojoExecutionException(String.format("%n%nFound more than one class that implements %s:%n%n%s%nYou can only have one per plugin project.", pluginClassName, sb));
        }

        try {
            Class<?> pluginClass = pluginImplClasses.get(0);
            Files.write(spiServicesFile.toPath(), pluginClass.getName().getBytes(UTF_8));
            getLog().info(String.format("Wrote %s to %s", pluginClass.getName(), spiServicesFile));
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to write services JAR file.", e);
        }
    }

    private URLClassLoader createCLFromCompileTimeDependencies()
            throws Exception
    {
        List<URL> classesUrls = new ArrayList<>();
        classesUrls.add(classesDirectory.toURI().toURL());
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null) {
                classesUrls.add(artifact.getFile().toURI().toURL());
            }
        }
        return new URLClassLoader(classesUrls.toArray(new URL[0]));
    }

    private List<Class<?>> findPluginImplClasses(URLClassLoader urlClassLoader)
            throws IOException, MojoExecutionException
    {
        List<Class<?>> implementations = new ArrayList<>();
        List<String> classes = FileUtils.getFileNames(classesDirectory, "**/*.class", null, false);
        for (String classPath : classes) {
            String className = classPath.substring(0, classPath.length() - 6).replace(File.separatorChar, '.');
            try {
                Class<?> pluginClass = urlClassLoader.loadClass(pluginClassName);
                Class<?> clazz = urlClassLoader.loadClass(className);
                if (isImplementation(clazz, pluginClass)) {
                    implementations.add(clazz);
                }
            }
            catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Failed to load class.", e);
            }
        }
        return implementations;
    }

    private static boolean isImplementation(Class<?> clazz, Class<?> pluginClass)
    {
        return pluginClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers());
    }
}
