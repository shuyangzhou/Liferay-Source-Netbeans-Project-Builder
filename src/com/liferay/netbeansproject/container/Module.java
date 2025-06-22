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

package com.liferay.netbeansproject.container;

import com.liferay.netbeansproject.util.HashUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Tom Wang
 */
public class Module implements Comparable<Module> {

	public static Module createModule(
			Path projectPath, Path modulePath,
			Set<Dependency> moduleDependencies, Set<Dependency> jarDependencies,
			Properties portalModuleDependencyProperties, Path trunkPath,
			boolean includeTomcatWorkJSP, Path portalPath, Set<String> portalPreModuleNames)
		throws IOException {

		if (jarDependencies == null) {
			jarDependencies = new HashSet<>();
		}

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (projectPath != null) {
			projectPath = projectPath.resolve(modulePath.getFileName());
		}

		Path resourcesPath = Paths.get(
			modulePath.toString(), "src", "main", "resources", "META-INF",
			"resources");

		Path jspPath = null;

		if (includeTomcatWorkJSP) {
			if (Files.exists(resourcesPath)) {
				Stream<Path> jspStream = Files.list(resourcesPath)
					.filter(fileName -> fileName.toString().endsWith(".jsp"));

				if (jspStream.count() > 0) {
					jspPath = Paths.get(
						trunkPath.toString(), "work",
						_getWorkPath(modulePath.resolve("bnd.bnd")));
				}
			}
		}

		boolean isTopLevel = false;

		if (portalPath.equals(modulePath.getParent())) {
			isTopLevel = true;
		}

		return new Module(
			projectPath, modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, true),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, false),
			_resolveResourcePath(modulePath, "testIntegration"),
			_resolveJmhPath(modulePath), jspPath,
			moduleDependencies, jarDependencies,
			_resolvePortalModuleDependencies(
				portalModuleDependencyProperties,
				String.valueOf(modulePath.getFileName()), portalPreModuleNames),
			_resolveJdkVersion(gradleFilePath, isTopLevel));
	}

	@Override
	public int compareTo(Module module) {
		String moduleName = getModuleName();

		return moduleName.compareTo(module.getModuleName());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Module)) {
			return false;
		}

		Module module = (Module)obj;

		if (Objects.equals(_modulePath, module._modulePath) &&
			Objects.equals(_sourcePath, module._sourcePath) &&
			Objects.equals(_sourceResourcePath, module._sourceResourcePath) &&
			Objects.equals(_testUnitPath, module._testUnitPath) &&
			Objects.equals(
				_testUnitResourcePath, module._testUnitResourcePath) &&
			Objects.equals(_testIntegrationPath, module._testIntegrationPath) &&
			Objects.equals(
				_testIntegrationResourcePath,
				module._testIntegrationResourcePath) &&
			Objects.equals(_jspPath, module._jspPath)) {

			return true;
		}

		return false;
	}

	public Set<Dependency> getJarDependencies() {
		return _jarDependencies;
	}

	public String getJdkVersion() {
		return _jdkVersion;
	}

	public Path getJmhPath() {
		return _jmhPath;
	}

	public Path getJspPath() {
		return _jspPath;
	}

	public Set<Dependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public String getModuleName() {
		Path fileName = _modulePath.getFileName();

		return fileName.toString();
	}

	public Path getModulePath() {
		return _modulePath;
	}

	public Set<String> getPortalModuleDependencies() {
		return _portalModuleDependencies;
	}

	public Path getSourcePath() {
		return _sourcePath;
	}

	public Path getSourceResourcePath() {
		return _sourceResourcePath;
	}

	public Path getTestIntegrationPath() {
		return _testIntegrationPath;
	}

	public Path getTestIntegrationResourcePath() {
		return _testIntegrationResourcePath;
	}

	public Path getTestUnitPath() {
		return _testUnitPath;
	}

	public Path getTestUnitResourcePath() {
		return _testUnitResourcePath;
	}

	@Override
	public int hashCode() {
		int hashCode = HashUtil.hash(0, _modulePath);

		hashCode = HashUtil.hash(hashCode, _sourcePath);
		hashCode = HashUtil.hash(hashCode, _sourceResourcePath);
		hashCode = HashUtil.hash(hashCode, _testUnitPath);
		hashCode = HashUtil.hash(hashCode, _testUnitResourcePath);
		hashCode = HashUtil.hash(hashCode, _testIntegrationPath);
		hashCode = HashUtil.hash(hashCode, _testIntegrationResourcePath);
		hashCode = HashUtil.hash(hashCode, _jspPath);

		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("{projectPath=");
		sb.append(_projectPath);
		sb.append(", modulePath=");
		sb.append(_modulePath);
		sb.append(", sourcePath=");
		sb.append(_sourcePath);
		sb.append(", sourceResourcePath=");
		sb.append(_sourceResourcePath);
		sb.append(", testUnitPath=");
		sb.append(_testUnitPath);
		sb.append(", testUnitResourcePath=");
		sb.append(_testUnitResourcePath);
		sb.append(", testIntegrationPath=");
		sb.append(_testIntegrationPath);
		sb.append(", testIntegrationResourcePath=");
		sb.append(_testIntegrationResourcePath);
		sb.append(", jspPath=");
		sb.append(_jspPath);
		sb.append(", moduleDependencies=");
		sb.append(_moduleDependencies);
		sb.append(", jarDependencies=");
		sb.append(_jarDependencies);
		sb.append("}");

		return sb.toString();
	}

	private static String _getWorkPath(Path bndPath) throws IOException {
		List<String> lines = Files.readAllLines(bndPath);

		String fileName = "";

		for (String line : lines) {
			if (line.startsWith("Bundle-SymbolicName")) {
				String[] split = StringUtil.split(line, ':');

				fileName = split[1].trim();
			}

			if (line.startsWith("Bundle-Version")) {
				String[] split = StringUtil.split(line, ':');

				fileName += "-" + split[1].trim();
			}
		}

		if (fileName.isEmpty()) {
			throw new IOException("Incorrect filename, check " + bndPath);
		}

		return fileName;
	}

	private static String _resolveJdkVersion(
			Path gradleFilePath, boolean isTopLevel)
		throws IOException {

		if (!Files.exists(gradleFilePath) || isTopLevel) {
			return "1.8";
		}

		List<String> lines = Files.readAllLines(gradleFilePath);

		for (String line : lines) {
			if (line.startsWith("sourceCompatibility")) {
				String[] split = StringUtil.split(line, '=');

				return StringUtil.extractQuotedText(split[1]);
			}
		}

		return "1.8";
	}

	private static Set<String> _resolvePortalModuleDependencies(
			Properties properties, String moduleName, Set<String> portalPreModuleNames)
		throws IOException {

		String dependencies = properties.getProperty(moduleName);

		if (dependencies == null || dependencies.isEmpty()) {
			if (moduleName.endsWith("-test")) {
				Set<String> dependencySet = new HashSet<>(portalPreModuleNames);

				dependencySet.add("portal-impl");
				dependencySet.add("portal-kernel");

				return dependencySet;
			}

			return Collections.emptySet();
		}

		StringBuilder sb = new StringBuilder();

		sb.append(dependencies);
		sb.append(',');
		sb.append(StringUtil.merge(portalPreModuleNames, ','));

		return new HashSet<>(Arrays.asList(StringUtil.split(sb.toString(), ',')));
	}

	private static Path _resolveJmhPath(Path modulePath) {
		Path resolvedJmhSrcPath = modulePath.resolve(
			Paths.get("src", "jmh", "java"));

		if (Files.exists(resolvedJmhSrcPath)) {
			return resolvedJmhSrcPath;
		}

		return null;
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		Path resolvedResourcePath = modulePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resolvedResourcePath)) {
			return resolvedResourcePath;
		}

		return null;
	}

	private static Path _resolveSourcePath(Path modulePath) {
		Path sourcePath = modulePath.resolve(
			Paths.get("docroot", "WEB-INF", "src"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve(Paths.get("src", "main", "java"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve("src");

		if (Files.exists(sourcePath.resolve("main")) ||
			Files.exists(sourcePath.resolve("test")) ||
			Files.exists(sourcePath.resolve("testIntegration"))) {

			return null;
		}

		return sourcePath;
	}

	private static Path _resolveTestPath(Path modulePath, boolean unit) {
		Path testPath = null;

		if (unit) {
			testPath = modulePath.resolve(Paths.get("src", "test", "java"));
		}
		else {
			testPath = modulePath.resolve(
				Paths.get("src", "testIntegration", "java"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		if (unit) {
			testPath = modulePath.resolve(Paths.get("test", "unit"));
		}
		else {
			testPath = modulePath.resolve(Paths.get("test", "integration"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		return null;
	}

	private Module(
		Path projectPath, Path modulePath, Path sourcePath,
		Path sourceResourcePath, Path testUnitPath, Path testUnitResourcePath,
		Path testIntegrationPath, Path testIntegrationResourcePath,
		Path jmhPath, Path jspPath, Set<Dependency> moduleDependencies,
		Set<Dependency> jarDependencies, Set<String> portalModuleDependencies,
		String jdkVersion) {

		_projectPath = projectPath;
		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_jmhPath = jmhPath;
		_jspPath = jspPath;
		_moduleDependencies = moduleDependencies;
		_jarDependencies = jarDependencies;
		_portalModuleDependencies = portalModuleDependencies;
		_jdkVersion = jdkVersion;

		if ((_testUnitPath != null) || (_testIntegrationPath != null)) {
			Path portalTest = _projectPath.resolve("portal-test");

			_moduleDependencies.add(
				new Dependency(portalTest, portalTest.resolve("src"), true));
		}
	}

	private final Set<Dependency> _jarDependencies;
	private final String _jdkVersion;
	private final Path _jmhPath;
	private final Path _jspPath;
	private final Set<Dependency> _moduleDependencies;
	private final Path _modulePath;
	private final Set<String> _portalModuleDependencies;
	private final Path _projectPath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;

}