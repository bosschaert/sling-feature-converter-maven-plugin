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
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    Collection<Artifact> getMatchingArtifacts(final MavenProject project,
            RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos) {
        // get artifacts depending on whether we exclude transitives or not
        final Set<Artifact> artifacts;
        // TODO: when I ran the tests the artifacts where only available in the Dependency Artifacts and
        //       getArtifacts() returned an empty set
        if (excludeTransitive) {
            // only direct dependencies, transitives excluded
            artifacts = project.getDependencyArtifacts();
        } else {
            // all dependencies, transitives included
            artifacts = project.getArtifacts();
        }

        // Sometimes artifacts don't have their 'file' attribute set, which we need. For those that don't, resolve them
        final Set<Artifact> fileArtifacts = new HashSet<>();
        for (Artifact a : artifacts) {
            if (a.getFile() != null) {
                fileArtifacts.add(a);
            } else {
                if (repoSystem != null && repoSession != null) {
                    // Resolving the artifact via Aether will fill in the file attribute
                    Artifact fileArt = resolveArtifact(repoSystem, repoSession, remoteRepos, a);
                    if (fileArt != null) {
                        fileArtifacts.add(fileArt);
                    } else {
                        throw new RuntimeException("Unable to resolve artifact: " + a);
                    }
                }
            }
        }

        // Add the project artifact itself to convert after building a content package
        if(moduleIsContentPackage) {
            Artifact projectArtifact = project.getArtifact();
            System.out.println("Project Artifact: " + projectArtifact);
            fileArtifacts.add(projectArtifact);
        }
        return getMatchingArtifacts(fileArtifacts);
    }

    private Artifact resolveArtifact(final RepositorySystem repoSystem, final RepositorySystemSession repoSession,
            final List<RemoteRepository> remoteRepos, final Artifact artifact) {
        try {
            // Get an Aether Artifact
            org.eclipse.aether.artifact.Artifact a = new org.eclipse.aether.artifact.DefaultArtifact(
                    artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getClassifier(), artifact.getType(),
                    artifact.getVersion());

            ArtifactRequest req = new ArtifactRequest(a, remoteRepos, null);
            ArtifactResult res = repoSystem.resolveArtifact(repoSession, req);

            if (res.isResolved()) {
                org.eclipse.aether.artifact.Artifact aetherArt = res.getArtifact();
                Artifact mavenArt = new DefaultArtifact(
                        aetherArt.getGroupId(), aetherArt.getArtifactId(), aetherArt.getVersion(), null, aetherArt.getExtension(),
                        aetherArt.getClassifier(), new DefaultArtifactHandler());
                mavenArt.setFile(aetherArt.getFile());
                return mavenArt;
            } else {
                return null;
            }
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
}
