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

import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tom Wang
 */
public class ModuleTest {

	@Before
	public void setup() throws IOException {
		_projectDependencyProperties = PropertiesUtil.loadProperties(
			Paths.get("portal-module-dependency.properties"));
	}

	@After
	public void cleanup() throws IOException {
		FileUtil.delete(_rootPath);
	}

	@Test
	public void testClassicModule() throws IOException {
		Path srcPath = _rootPath.resolve("src");
		Path unitPath = _rootPath.resolve(Paths.get("test", "unit"));
		Path integrationPath = _rootPath.resolve(
			Paths.get("test", "integration"));

		_createDirectories(srcPath, unitPath, integrationPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertEquals(srcPath, module.getSourcePath());
		Assert.assertEquals(unitPath, module.getTestUnitPath());
		Assert.assertEquals(integrationPath, module.getTestIntegrationPath());
	}

	@Test
	public void testMavenModule() throws IOException {
		Path srcPath = _rootPath.resolve(Paths.get("src", "main", "java"));
		Path unitPath = _rootPath.resolve(Paths.get("src", "test", "java"));
		Path integrationPath = _rootPath.resolve(
			Paths.get("src", "testIntegration", "java"));

		_createDirectories(srcPath, unitPath, integrationPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertEquals(srcPath, module.getSourcePath());
		Assert.assertEquals(unitPath, module.getTestUnitPath());
		Assert.assertEquals(	integrationPath, module.getTestIntegrationPath());
	}

	@Test
	public void testMavenTestModuleUnit() throws IOException {
		Path unitTestPath = _rootPath.resolve(Paths.get("src", "test", "java"));

		Files.createDirectories(unitTestPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertNull(module.getSourcePath());
		Assert.assertEquals(unitTestPath, module.getTestUnitPath());
	}

	@Test
	public void testMavenTestModuleIntegration() throws IOException {
		Path integrationTestPath = _rootPath.resolve(
			Paths.get("src", "testIntegration", "java"));

		Files.createDirectories(integrationTestPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertNull(module.getSourcePath());
		Assert.assertEquals(
			integrationTestPath, module.getTestIntegrationPath());
	}

	@Test
	public void testMavenTestModuleUnitAndIntegration() throws IOException {
		Path unitTestPath = _rootPath.resolve(Paths.get("src", "test", "java"));
		Path integrationTestPath = _rootPath.resolve(
			Paths.get("src", "testIntegration", "java"));

		_createDirectories(unitTestPath, integrationTestPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertNull(module.getSourcePath());
		Assert.assertEquals(unitTestPath, module.getTestUnitPath());
		Assert.assertEquals(
			integrationTestPath, module.getTestIntegrationPath());
	}

	@Test
	public void testWarModule() throws IOException {
		Path srcPath = _rootPath.resolve(
			Paths.get("docroot", "WEB-INF", "src"));

		Files.createDirectories(srcPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertEquals(srcPath, module.getSourcePath());
	}

	@Test
	public void testResources() throws IOException {
		Path srcResourcesPath = _rootPath.resolve(
			Paths.get("src", "main", "resources"));
		Path testUnitResourcesPath = _rootPath.resolve(
			Paths.get("src", "test", "resources"));
		Path testIntegrationResourcesPath = _rootPath.resolve(
			Paths.get("src", "testIntegration", "resources"));

		_createDirectories(
			srcResourcesPath, testUnitResourcesPath,
			testIntegrationResourcesPath);

		Module module = Module.createModule(
			_rootPath, _rootPath, new HashSet<>(), new HashSet<>(),
			_projectDependencyProperties, _rootPath, false,
			_rootPath.getParent(), Collections.emptySet());

		Assert.assertEquals(
			srcResourcesPath, module.getSourceResourcePath());
		Assert.assertEquals(
			testUnitResourcesPath, module.getTestUnitResourcePath());
		Assert.assertEquals(
			testIntegrationResourcesPath,
			module.getTestIntegrationResourcePath());
	}

	private static void _createDirectories(Path... paths) throws IOException {
		for (Path path : paths) {
			Files.createDirectories(path);
		}
	}

	private static Path _rootPath = Paths.get("test-out", "Unit-Test-Model");
	private static Properties _projectDependencyProperties;
}