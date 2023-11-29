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

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Coordinate;
import com.liferay.netbeansproject.container.Dependency;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Tom Wang
 */
public class ModuleUtil {

	public static Set<Dependency> getPortalLibJars(Path portalPath)
		throws IOException {

		Set<Dependency> jarSet = new TreeSet<>();

		Properties developmentLibProperties = PropertiesUtil.loadProperties(
			portalPath.resolve("lib/development/dependencies.properties"));

		for (Object value : developmentLibProperties.values()) {
			Dependency dependency = MavenUtil.toDependency(new Coordinate(String.valueOf(value)));

			if (Files.exists(dependency.getPath())) {
				jarSet.add(dependency);
			}
		}

		Properties portalLibProperties = PropertiesUtil.loadProperties(
			portalPath.resolve("lib/portal/dependencies.properties"));

		for (Object value : portalLibProperties.values()) {
			Dependency dependency = MavenUtil.toDependency(new Coordinate(String.valueOf(value)));

			if (Files.exists(dependency.getPath())) {
				jarSet.add(dependency);
			}
		}

		return jarSet;
	}

	public static String getSymbolicName(Path path) throws IOException {
		Path bndPath = path.resolve("bnd.bnd");

		if (!Files.exists(bndPath)) {
			return null;
		}

		for (String line : Files.readAllLines(bndPath)) {
			if (!line.startsWith("Bundle-SymbolicName")) {
				continue;
			}

			String[] lineSplit = StringUtil.split(line, ": ");

			if (lineSplit[1].equals("${manifest.bundle.symbolic.name}")) {
				String symbolicName = String.valueOf(path.getFileName());

				return "com.liferay.".concat(symbolicName.replace('-', '.'));
			}

			return lineSplit[1];
		}

		throw new IllegalArgumentException(
			"Cannot find symbolic name in " + bndPath);
	}

}