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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 检查spi依赖
 */

@Mojo(name = "spi-dependencies-check",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class SpiDependencyChecker extends AbstractMojo {

    @Parameter(defaultValue = "org.apache.dolphinscheduler")
    private String spiGroupId;

    @Parameter(defaultValue = "dolphinscheduler-spi")
    private String spiArtifactId;

    @Parameter(defaultValue = "false")
    private boolean skipCheckSpiDependencies;

    @Parameter(defaultValue = "${project}")
    private MavenProject mavenProject;

    @Parameter
    private final Set<String> allowedProvidedDependencies = new HashSet<>();

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySession;

    @Component
    private RepositorySystem repositorySystem;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipCheckSpiDependencies) {
            getLog().info("Skipping Dolphinscheduler SPI dependency checks");
            return;
        }

        Set<String> spiDependencies = getTheSpiDependencies();
        getLog().debug("SPI dependencies: " + spiDependencies);

        for (Artifact artifact : mavenProject.getArtifacts()) {
            if (isSpiArtifact(artifact)) {
                continue;
            }
            String name = artifact.getGroupId() + ":" + artifact.getArtifactId();
            if (spiDependencies.contains(name)) {
                if (!"jar".equals(artifact.getType())) {
                    throw new MojoExecutionException(String.format("%n%nDolphinscheduler plugin dependency %s must have type 'jar'.", name));
                }
                if (artifact.getClassifier() != null) {
                    throw new MojoExecutionException(String.format("%n%nDolphinscheduler plugin dependency %s must not have a classifier.", name));
                }
                if (!"provided".equals(artifact.getScope())) {
                    throw new MojoExecutionException(String.format("%n%nDolphinscheduler plugin dependency %s must have scope 'provided'. It is part of the SPI and will be provided at runtime.", name));
                }
            }
            else if ("provided".equals(artifact.getScope()) && !allowedProvidedDependencies.contains(name)) {
                throw new MojoExecutionException(String.format("%n%nDolphinscheduler plugin dependency %s must not have scope 'provided'. It is not part of the SPI and will not be available at runtime.", name));
            }
        }
    }

    private Set<String> getTheSpiDependencies()
            throws MojoExecutionException
    {
        return getArtifactDependencies(getSpiDependency())
                .getRoot().getChildren().stream()
                .filter(node -> !node.getDependency().isOptional())
                .map(DependencyNode::getArtifact)
                .map(artifact -> artifact.getGroupId() + ":" + artifact.getArtifactId())
                .collect(Collectors.toSet());
    }

    private CollectResult getArtifactDependencies(Artifact artifact)
            throws MojoExecutionException
    {
        try {
            org.eclipse.aether.artifact.Artifact artifact1 = aetherArtifact(artifact);
            Dependency projectDependency = new Dependency(artifact1, null);
            return repositorySystem.collectDependencies(repositorySession, new CollectRequest(projectDependency, null));
        }
        catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Failed to resolve dependencies.", e);
        }
    }

    private Artifact getSpiDependency()
            throws MojoExecutionException
    {
        for (Artifact artifact : mavenProject.getArtifacts()) {
            if (!isSpiArtifact(artifact)) {
                continue;
            }

            if (!"provided".equals(artifact.getScope())) {
                throw new MojoExecutionException(String.format("DolphinScheduler plugin dependency %s must have scope 'provided'.", spiName()));
            }
            return artifact;
        }
        throw new MojoExecutionException(String.format("DolphinScheduler plugin must depend on %s.", spiName()));
    }

    private boolean isSpiArtifact(Artifact artifact)
    {
        return spiGroupId.equals(artifact.getGroupId())
                && spiArtifactId.equals(artifact.getArtifactId())
                && "jar".equals(artifact.getType())
                && (artifact.getClassifier() == null);
    }

    private String spiName()
    {
        return spiGroupId + ":" + spiArtifactId;
    }

    private static org.eclipse.aether.artifact.Artifact aetherArtifact(Artifact artifact)
    {
        return new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getType(),
                artifact.getVersion());
    }
}
