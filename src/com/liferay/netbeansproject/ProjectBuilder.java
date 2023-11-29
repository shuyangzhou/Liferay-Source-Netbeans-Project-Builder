/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.netbeansproject;

import com.liferay.netbeansproject.container.Dependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.MavenUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class ProjectBuilder {

	public static void main(String[] args) throws Exception {
		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		String[] portalDirs = StringUtil.split(
			PropertiesUtil.getRequiredProperty(buildProperties, "portal.dirs"),
			',');

		Path projectDirPath = Paths.get(
			PropertiesUtil.getRequiredProperty(buildProperties, "project.dir"));

		String ignoredDirs = PropertiesUtil.getRequiredProperty(
			buildProperties, "ignored.dirs");

		boolean includeJsps = Boolean.valueOf(
			PropertiesUtil.getRequiredProperty(
				buildProperties, "include.generated.jsp.servlet"));

		ProjectBuilder projectBuilder = new ProjectBuilder();

		for (String portalDir : portalDirs) {
			Path portalDirPath = Paths.get(portalDir);

			MavenUtil.buildCache(portalDirPath, buildProperties);

			Properties appServerProperties = new Properties();

			Path trunkPath = null;

			if (includeJsps) {
				appServerProperties = PropertiesUtil.loadProperties(
					portalDirPath.resolve("app.server.properties"));

				trunkPath = Paths.get(
					appServerProperties.getProperty("app.server.parent.dir"));
			}

			projectBuilder.scanPortal(
				projectDirPath.resolve(portalDirPath.getFileName()),
				portalDirPath, ignoredDirs,	trunkPath,
				appServerProperties.getProperty("app.server.tomcat.version"),
				includeJsps);
		}
	}

	public void scanPortal(
			final Path projectPath, Path portalPath, String ignoredDirs,
			Path trunkPath, String tomcatVersion, boolean includeJsps)
		throws Exception {

		final Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		final Properties portalModuleDependencyProperties =
			PropertiesUtil.loadProperties(
				Paths.get("portal-module-dependency.properties"));

		final Set<String> moduleNames = new HashSet<>();

		final Map<String, Path> moduleProjectPaths = new HashMap<>();

		final Set<Path> modulePaths = new HashSet<>();

		Set<String> portalPreModuleNames = new HashSet<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					if (ignoredDirSet.contains(
							String.valueOf(path.getFileName()))) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src")) && !path.getParent().getFileName().toString().equals("third-party") && !path.getParent().getFileName().toString().equals("shared-dependencies")) {
						return FileVisitResult.CONTINUE;
					}

					if (path.endsWith("WEB-INF")) {
						path = path.getParent();
						path = path.getParent();
					}

					String moduleName = String.valueOf(path.getFileName());

					moduleNames.add(moduleName);

					String symbolicName = ModuleUtil.getSymbolicName(path);

					if (symbolicName != null) {
						moduleProjectPaths.put(
							symbolicName,
							Paths.get(
								"modules", String.valueOf(path.getFileName())));
					}

					if (Files.exists(path.resolve(".lfrbuild-portal-pre"))) {
						portalPreModuleNames.add(moduleName);
					}

					modulePaths.add(path);

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		Map<Path, Set<Dependency>> moduleDependenciesMap = new HashMap<>();

		for (Path modulePath : modulePaths) {
			moduleDependenciesMap.put(
				modulePath,
				GradleUtil.getModuleDependencies(
					modulePath, moduleProjectPaths));
		}

		FileUtil.delete(projectPath);

		Set<Dependency> portalLibJars = ModuleUtil.getPortalLibJars(portalPath);

		for (Path modulePath : modulePaths) {
			Module module = Module.createModule(
				projectPath.resolve("modules"), modulePath,
				moduleDependenciesMap.get(modulePath),
				MavenUtil.getDependencies(modulePath.resolve("build.gradle")),
				portalModuleDependencyProperties, trunkPath, includeJsps,
				portalPath, portalPreModuleNames);

			CreateModule.createModule(
				module, projectPath, portalLibJars, portalPath);
		}

		CreateUmbrella.createUmbrella(
			portalPath, moduleNames, projectPath.resolve("umbrella"), trunkPath,
			tomcatVersion);
	}

}