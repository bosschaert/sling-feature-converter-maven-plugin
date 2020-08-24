[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-feature-converter-maven-plugin/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-feature-converter-maven-plugin/job/master/) [![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-feature-converter-maven-plugin/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-feature-converter-maven-plugin/job/master/test/?width=800&height=600) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-feature-converter-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-feature-converter-maven-plugin) [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-feature-converter-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-feature-converter-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/sling-feature-converter-maven-plugin/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22sling-feature-converter-maven-plugin%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/sling-feature-converter-maven-plugin.svg)](https://www.javadoc.io/doc/org.apache.sling/sling-feature-converter-maven-plugin) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Feature Converter Maven Plugin

This module is part of the [Apache Sling](https://sling.apache.org) project.

This Apache Maven plugin provides the means to convert both Content Packages as well as
Provisioning Models into Feature Models through a POM file.

# Introduction

This plugin is intended to convert:
* Content Packages
* Provisioning Models
into Feature Models so that it can be used by the Sling Feature Maven
Plugin to assemble and run.
 
This plugin is a wrapper for the Sling Feature Content Package Converter
**sling-org-apache-sling-feature-cpconverter** and the Sling Feature Model
Converter **sling-org-apache-sling-feature-modelconveter** to convert
source files into Feature Models inside a POM.

This plugin is normally used to convert Sling into its Feature Model
version and to convert Content Packages into Feature Models so that they
can be handled by the Sling Feature Maven Plugin and eventually launched.

## Supported goals

These Goals are support in this plugin:

* **convert-cp**: Converts Content Packages into Feature Model files
and its converted ZIP files
* **convert-pom**: Converts Provisioning Models into Feature Models

See the Site documentation for further info.

## Content Package Handling

The installation of Converted Content Packages has been difficult at times.

These are the things so far that should help making this easier:
1. Make sure that you use the latest code base
2. Make sure that during the launch:
    1. Sling Feature Extension Content is added first as Plugin Dependency
       (any extension should be added before the Launcher)
    2. Add Sling Feature Launcher as Plugin Dependency afterwards
    3. Make sure that the **JCR Package Init** is added as Feature
       (see Site Documentation **Usage**)
3. After the Feature is up and running log into OSGi Console and check
   the **OSGi Installer** page to make sure there are no **Untransformed
   Resources**
4. During development make sure that the **Launcher** folder is deleted
   periodically to avoid side effects from previous launches
