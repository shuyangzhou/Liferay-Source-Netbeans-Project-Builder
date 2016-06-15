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
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class ProcessGradle {

	public static Map<String, List<JarDependency>> processGradle(
			Path portalDirPath, Path projectDirPath, Path workDirPath,
			boolean displayGradleProcessOutput)
		throws Exception {

		Path dependenciesDirPath = projectDirPath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		List<String> gradleTask = new ArrayList<>();

		gradleTask.add(String.valueOf(portalDirPath.resolve("gradlew")));
		gradleTask.add("--parallel");
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("-p");
		gradleTask.add(String.valueOf(portalDirPath.resolve("modules")));
		gradleTask.add(_getTaskName(portalDirPath, workDirPath));
		gradleTask.add(
			"-PdependencyDirectory=".concat(dependenciesDirPath.toString()));

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Map<String, String> env = processBuilder.environment();

		env.put("GRADLE_OPTS", "-Xmx2g");

		Process process = processBuilder.start();

		if (displayGradleProcessOutput) {
			String line = null;

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(
				BufferedReader br = new BufferedReader(
					new InputStreamReader(process.getErrorStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}
		}

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(
				"Process " + processBuilder.command() + " failed with " +
					exitCode);
		}

		final Map<String, List<JarDependency>> dependenciesMap =
			new HashMap<>();

		Files.walkFileTree(
			dependenciesDirPath,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					List<JarDependency> jarDependencies = new ArrayList<>();

					Properties dependencies = PropertiesUtil.loadProperties(
						path);

					for (String jar :
							StringUtil.split(
								dependencies.getProperty("compile"), ':')) {

						jarDependencies.add(
							new JarDependency(Paths.get(jar), false));
					}

					for (String jar :
							StringUtil.split(
								dependencies.getProperty("compileTest"), ':')) {

						jarDependencies.add(
							new JarDependency(Paths.get(jar), true));
					}

					Path moduleName = path.getFileName();

					dependenciesMap.put(moduleName.toString(), jarDependencies);

					return FileVisitResult.CONTINUE;
				}

			});

		return dependenciesMap;
	}

	private static String _getTaskName(Path portalDirPath, Path workDirPath) {
		Path modulesPath = portalDirPath.resolve("modules");

		Path relativeWorkPath = modulesPath.relativize(workDirPath);

		if (relativeWorkPath.getNameCount() == 0) {
			return "printDependencies";
		}

		String relativeWorkPathString = relativeWorkPath.toString();

		relativeWorkPathString = relativeWorkPathString.replace('/', ':');

		return relativeWorkPathString.concat(":").concat("printDependencies");
	}

}