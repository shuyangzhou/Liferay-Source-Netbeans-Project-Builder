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

import com.liferay.netbeansproject.container.JarDependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.GradleUtil;
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
import java.util.List;
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

		Path projectPath = Paths.get(
			PropertiesUtil.getRequiredProperty(buildProperties, "project.dir"));

		for (String portal : StringUtil.split(
				PropertiesUtil.getRequiredProperty(
					buildProperties, "portal.dirs"),
				',')) {

			Path portalPath = Paths.get(portal);

			Path portalProjectPath = projectPath.resolve(
				portalPath.getFileName());

			ProjectBuilder projectBuilder = new ProjectBuilder();

			projectBuilder.scanPortal(
				portalProjectPath, portalPath, buildProperties);
		}
	}

	public void scanPortal(
			final Path projectPath, Path portalPath, Properties buildProperties)
		throws Exception {

		String ignoredDirs = PropertiesUtil.getRequiredProperty(
			buildProperties, "ignored.dirs");

		String projectName = PropertiesUtil.getRequiredProperty(
			buildProperties, "project.name");

		FileUtil.delete(projectPath);

		final Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		final Map<String, List<JarDependency>> jarDependenciesMap =
			GradleUtil.getJarDependencies(
				portalPath, portalPath.resolve("modules"),
				Boolean.valueOf(
					buildProperties.getProperty(
						"display.gradle.process.output")));

		final Properties projectDependencyProperties =
			PropertiesUtil.loadProperties(
				Paths.get("project-dependency.properties"));

		final Map<Path, Module> moduleMap = new HashMap<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path fileNamePath = path.getFileName();

					String fileName = fileNamePath.toString();

					if (ignoredDirSet.contains(fileName)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					Module module = Module.createModule(
						projectPath.resolve(
							Paths.get(
								"modules", ModuleUtil.getModuleName(path))),
						path, jarDependenciesMap.get(fileName),
						projectDependencyProperties);

					moduleMap.put(module.getModulePath(), module);

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		String excludedTypes = buildProperties.getProperty("exclude.types");

		String portalLibJars = ModuleUtil.getPortalLibJars(portalPath);

		for (Module module : moduleMap.values()) {
			CreateModule.createModule(
				module, projectPath, excludedTypes, portalLibJars, portalPath);
		}

		CreateUmbrella.createUmbrella(
			portalPath, projectName,
			PropertiesUtil.getProperties(
				buildProperties, "umbrella.source.list"),
			excludedTypes, moduleMap.keySet(), projectPath);
	}

}