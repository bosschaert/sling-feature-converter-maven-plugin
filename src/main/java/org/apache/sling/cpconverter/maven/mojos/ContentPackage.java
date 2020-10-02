/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cpconverter.maven.mojos;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContentPackage {

    /**
     * The content package Group Id
     */
    private String groupId = "";

    /**
     * The content package Artifact Id
     */
    private String artifactId = "";

    private List<String> types = new ArrayList(Arrays.asList("zip"));

    // TODO: Classifier should not be set as we have the one from the converter
    private String classifier = "";

    private boolean excludeTransitive;

    private boolean moduleIsContentPackage;

    private ArtifactHandler artifactHandler = new NullArtifactHandler(); // TODO

    public void setGroupId(String groupId) {
        this.groupId = groupId == null ? "" : groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId == null ? "" : artifactId;
    }

    public void setExcludeTransitive(boolean excludeTransitive) {
        this.excludeTransitive = excludeTransitive;
    }

    public boolean isExcludeTransitive() {
        return excludeTransitive;
    }

    public void setModuleIsContentPackage(boolean moduleIsContentPackage) {
        this.moduleIsContentPackage = moduleIsContentPackage;
        if(this.moduleIsContentPackage) {
            types.add("content-package");
        } else {
            types.remove("content-package");
        }
    }

    public boolean isModuleIsContentPackage() {
        return moduleIsContentPackage;
    }

    public Collection<Artifact> getMatchingArtifacts(final MavenProject project, RepositorySystem repoSystem, RepositorySystemSession repoSession) {
        /* TODO why doesn't this work?
        // Get artifacts via Aether
        org.eclipse.aether.artifact.Artifact art = new org.eclipse.aether.artifact.DefaultArtifact(
                project.getGroupId(), project.getArtifactId(), project.getArtifact().getType(), project.getVersion());
        final Set<Artifact> artifacts = getDependencies(repoSystem, repoSession, art, !excludeTransitive);
        */
        List<org.apache.maven.model.Dependency> deps = project.getDependencies();
        List<org.eclipse.aether.artifact.Artifact> arts = depsToArtifacts(deps);

        Set<Artifact> artifacts = new HashSet<>();
        for (org.eclipse.aether.artifact.Artifact art : arts) {
//            artifacts.addAll(getDependencies(repoSystem, repoSession, art, !excludeTransitive));
            artifacts.add(resolveArtifact(repoSystem, repoSession, art));
        }

        // Add the project artifact itself to convert after building a content package
        if(moduleIsContentPackage) {
            Artifact projectArtifact = project.getArtifact();
            System.out.println("Project Artifact: " + projectArtifact);
            artifacts.add(projectArtifact);
        }
        return getMatchingArtifacts(artifacts);
    }

    private List<org.eclipse.aether.artifact.Artifact> depsToArtifacts(List<org.apache.maven.model.Dependency> deps) {
        return deps
            .stream()
            .map(d -> new org.eclipse.aether.artifact.DefaultArtifact(
                    d.getGroupId(), d.getArtifactId(),
                    d.getClassifier(), d.getType(),
                    d.getVersion()))
            .collect(Collectors.toList());

        /*
        org.eclipse.aether.artifact.Artifact a = null;

        a = new org.eclipse.aether.artifact.DefaultArtifact(
                a.getGroupId(), a.getArtifactId(), a.getVersion(), null,
                a.getExtension(), a.getClassifier(), artifactHandler);

        return deps
            .stream()
            .map(d -> new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(),
                    null, d.getType(), d.getClassifier(), artifactHandler))
            .collect(Collectors.toList());
            */
    }

    private Artifact resolveArtifact(final RepositorySystem repoSystem, final RepositorySystemSession repoSession,
            final org.eclipse.aether.artifact.Artifact artifact) {
        try {
            ArtifactRequest req = new ArtifactRequest(artifact, null, null);
            ArtifactResult res = repoSystem.resolveArtifact(repoSession, req);

            if (res.isResolved()) {
                org.eclipse.aether.artifact.Artifact aetherArt = res.getArtifact();
                Artifact mavenArt = new DefaultArtifact(
                        aetherArt.getGroupId(), aetherArt.getArtifactId(), aetherArt.getVersion(), null, aetherArt.getExtension(),
                        aetherArt.getClassifier(), artifactHandler);
                mavenArt.setFile(aetherArt.getFile());
                return mavenArt;
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Set<Artifact> getDependencies(final RepositorySystem repoSystem, final RepositorySystemSession repoSession,
            final org.eclipse.aether.artifact.Artifact artifact, final boolean includeTransitive) {
        try {
            Dependency dep = new Dependency(artifact, "compile");
            CollectRequest req = new CollectRequest(dep, null);
            DependencyResult deps = repoSystem.resolveDependencies(repoSession, new DependencyRequest(req, null));
            DependencyNode root = deps.getRoot();

            Set<Artifact> result = new HashSet<>();
            for (DependencyNode child : root.getChildren()) {
                org.eclipse.aether.artifact.Artifact aetherArt = child.getArtifact();

                Artifact mavenArt = new DefaultArtifact(
                        aetherArt.getGroupId(), aetherArt.getArtifactId(), aetherArt.getVersion(), null, aetherArt.getExtension(),
                        aetherArt.getClassifier(), artifactHandler);
                mavenArt.setFile(aetherArt.getFile());
                result.add(mavenArt);

                if (includeTransitive) {
                    result.addAll(getDependencies(repoSystem, repoSession, aetherArt, includeTransitive));
                }
            }
            return result;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Artifact> getMatchingArtifacts(final Collection<Artifact> artifacts) {
        final List<Artifact> matches = new ArrayList<Artifact>();
        for (Artifact artifact : artifacts) {
            System.out.println("Check Artifact: " + artifact);
            System.out.println("Check Artifact Group: " + artifact.getGroupId());
            System.out.println("Check Artifact Artifact: " + artifact.getArtifactId());
            System.out.println("Check Artifact Type: " + artifact.getType());
            System.out.println("Check Artifact Classifier: " + artifact.getClassifier());
            if(groupId.equals(artifact.getGroupId())
                && artifactId.equals(artifact.getArtifactId())
                && types.contains(artifact.getType())
                && (classifier.equals(artifact.getClassifier()) || (classifier.equals("") && artifact.getClassifier() == null))
            ) {
                matches.add(artifact);
            }
        }
        return matches;
    }

    @Nonnull
    public StringBuilder toString(@Nullable StringBuilder builder) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        builder.append("groupId=").append(groupId).append(",");
        builder.append("artifactId=").append(artifactId).append(",");

        if (types != null) {
            builder.append("type='").append(types).append("',");
        }
        if (classifier != null) {
            builder.append("classifier=").append(classifier).append(",");
        }
        builder.append(",excludeTransitive=").append(excludeTransitive);
        return builder;
    }

    public String toString() {
        return toString(null).toString();
    }

    private static class NullArtifactHandler implements ArtifactHandler {
        // TODO this is a temp workaround

        @Override
        public String getExtension() {
            return null;
        }

        @Override
        public String getDirectory() {
            return null;
        }

        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public String getPackaging() {
            return null;
        }

        @Override
        public boolean isIncludesDependencies() {
            return false;
        }

        @Override
        public String getLanguage() {
            return null;
        }

        @Override
        public boolean isAddedToClasspath() {
            return false;
        }
    }
}
