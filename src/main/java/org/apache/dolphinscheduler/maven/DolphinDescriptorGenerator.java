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
 * Mojo that generates the service descriptor JAR for DolphinScheduler plugins.
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
        File servicesFile = new File(servicesDirectory, pluginClassName);

        // If users have already provided their own service file then we will not overwrite it
        if (servicesFile.exists()) {
            return;
        }

        if (!servicesFile.getParentFile().exists()) {
            mkdirs(servicesFile.getParentFile());
        }

        List<Class<?>> pluginImplClasses;
        try {
            URLClassLoader loader = createCLFromCompileTimeDependencies();
            pluginImplClasses = findPluginImplClasses(loader);
        }
        catch (Exception e) {
            throw new MojoExecutionException(String.format("%n%nError scanning for classes implementing %s.", pluginClassName), e);
        }
        if (pluginImplClasses.isEmpty()) {
            throw new MojoExecutionException(String.format("%n%nYou must have at least one class that implements %s.", pluginClassName));
        }

        if (pluginImplClasses.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (Class<?> pluginClass : pluginImplClasses) {
                sb.append(pluginClass.getName()).append(LS_ALIAS);
            }
            throw new MojoExecutionException(String.format("%n%nYou have more than one class that implements %s:%n%n%s%nYou can only have one per plugin project.", pluginClassName, sb));
        }

        try {
            Class<?> pluginClass = pluginImplClasses.get(0);
            Files.write(servicesFile.toPath(), pluginClass.getName().getBytes(UTF_8));
            getLog().info(String.format("Wrote %s to %s", pluginClass.getName(), servicesFile));
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to write services JAR file.", e);
        }
    }

    private URLClassLoader createCLFromCompileTimeDependencies()
            throws Exception
    {
        List<URL> urls = new ArrayList<>();
        urls.add(classesDirectory.toURI().toURL());
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    private List<Class<?>> findPluginImplClasses(URLClassLoader searchRealm)
            throws IOException, MojoExecutionException
    {
        List<Class<?>> implementations = new ArrayList<>();
        List<String> classes = FileUtils.getFileNames(classesDirectory, "**/*.class", null, false);
        for (String classPath : classes) {
            String className = classPath.substring(0, classPath.length() - 6).replace(File.separatorChar, '.');
            try {
                Class<?> pluginClass = searchRealm.loadClass(pluginClassName);
                Class<?> clazz = searchRealm.loadClass(className);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void mkdirs(File file)
            throws MojoExecutionException
    {
        file.mkdirs();
        if (!file.isDirectory()) {
            throw new MojoExecutionException(String.format("%n%nFailed to create directory: %s", file));
        }
    }

    private static boolean isImplementation(Class<?> clazz, Class<?> pluginClass)
    {
        return pluginClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers());
    }
}
