/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Coordinate;
import com.liferay.netbeansproject.container.Dependency;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shuyang Zhou
 */
public class MavenUtil {

	public static Set<Dependency> getDependencies(Path buildGradlePath) {
		Set<Dependency> dependencies = new HashSet<>();

		List<Coordinate> coordinates = _mavenCoordinates.get(buildGradlePath);

		if (coordinates != null) {
			for (Coordinate coordinate : coordinates) {
				dependencies.add(coordinate.toDependency(_cachePath));
			}
		}

		for (Coordinate coordinate : _coordinateTestWhitelist) {
			dependencies.add(coordinate.toDependency(_cachePath));
		}

		return dependencies;
	}

	private static final Path _cachePath = Paths.get(".m2-cache");

	public static Dependency toDependency(Coordinate coordinate) {
		return coordinate.toDependency(_cachePath);
	}

	public static boolean resolve(Coordinate coordinate)
		throws IOException {

		String filePath = coordinate.getFilePath();

		Path binaryPath = _cachePath.resolve(filePath);

		if (Files.exists(binaryPath)) {
			return false;
		}

		String location = _toLocation(filePath);

		try {
			_downloadFile(filePath, location);

			_downloadSourcesFile(filePath, location);
		}
		catch (IOException ioException) {
			_logger.log(Level.SEVERE, "Unable to resolve " + coordinate + " from " + location);
		}

		return true;
	}

	private static String _toLocation(String filePath) throws IOException {
		URL url = new URL(_SEARCH_URL.concat(filePath));

		for (int i = 0; i < 10; i++) {
			HttpURLConnection httpURLConnection =
				(HttpURLConnection)url.openConnection();

			httpURLConnection.setInstanceFollowRedirects(false);

			int code = httpURLConnection.getResponseCode();

			if (code == HttpURLConnection.HTTP_MOVED_TEMP) {
				return httpURLConnection.getHeaderField("Location");
			}

			_logger.log(
				Level.WARNING, "Retrying " + i + " for " + url +
					" due to response code " + code);
		}

		throw new IllegalStateException("Unable to get location for " + url);
	}

	private static Path _downloadSourcesFile(
		String filePath, String urlString) {

		String sourceURLString = _toSources(urlString);

		try {
			return _downloadFile(_toSources(filePath), sourceURLString);
		}
		catch (IOException ioException) {
			_logger.log(
				Level.WARNING, "Unable to get source from " + sourceURLString);

			return null;
		}
	}
	private static Path _downloadFile(String filePath, String urlString)
		throws IOException {

		Path binaryPath = _cachePath.resolve(filePath);

		if (Files.exists(binaryPath)) {
			return binaryPath;
		}

		URL url = new URL(urlString);

		try (InputStream inputStream = url.openStream()) {
			Files.createDirectories(binaryPath.getParent());

			Files.copy(inputStream, binaryPath);
		}

		return binaryPath;
	}

	private static String _toSources(String value) {
		return value.replace(".jar", "-sources.jar");
	}

	private static final Map<Path, List<Coordinate>> _mavenCoordinates =
		new TreeMap<>();

	public static void buildCache(Path portalPath, Properties buildProperties)
		throws IOException {

		for (String line : StringUtil.split(
			PropertiesUtil.getRequiredProperty(
				buildProperties, "coordinate.blacklist"), ',')) {

			_coordinateBlacklist.add(new Coordinate(line.trim()));
		}

		for (String line : StringUtil.split(
			PropertiesUtil.getRequiredProperty(
				buildProperties, "coordinate.test.whitelist"), ',')) {

			Coordinate coordinate = new Coordinate(line.trim());

			coordinate.setTest(true);

			_coordinateTestWhitelist.add(coordinate);
		}

		Properties developmentLibProperties = PropertiesUtil.loadProperties(
			portalPath.resolve("lib/development/dependencies.properties"));

		for (Object value : developmentLibProperties.values()) {
			Coordinate coordinate = new Coordinate(String.valueOf(value));

			coordinate.setTest(true);

			_coordinateTestWhitelist.add(coordinate);
		}

		_coordinateTestWhitelist.removeAll(_coordinateBlacklist);

		String ignoredDirs = PropertiesUtil.getRequiredProperty(
			buildProperties, "ignored.dirs");

		Set<String> ignoredDirSet = new HashSet<>(
			Arrays.asList(StringUtil.split(ignoredDirs, ',')));

		long startTime = System.currentTimeMillis();

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

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path filePath, BasicFileAttributes basicFileAttributes)
					throws IOException {

					String fileName = String.valueOf(filePath.getFileName());

					if (fileName.equals("build.gradle")) {
						_mavenCoordinates.put(
							filePath, _parseCoordinates(filePath));
					}

					return FileVisitResult.CONTINUE;
				}

			}
		);

		System.out.println(
			"Scanned " + _mavenCoordinates.size() + " build.gradle in " +
				(System.currentTimeMillis() - startTime) + "ms");

		Set<Coordinate> mergedCoordinates = new TreeSet<>();

		for (List<Coordinate> coordinates : _mavenCoordinates.values()) {
			mergedCoordinates.addAll(coordinates);
		}

		Properties portalLibProperties = PropertiesUtil.loadProperties(
			portalPath.resolve("lib/portal/dependencies.properties"));

		for (Object value : portalLibProperties.values()) {
			Coordinate coordinate = new Coordinate(String.valueOf(value));

			mergedCoordinates.add(coordinate);
		}

		mergedCoordinates.addAll(_coordinateTestWhitelist);

		mergedCoordinates.removeAll(_coordinateBlacklist);		

		System.out.println(
			"Found " + mergedCoordinates.size() + " unique coordinates");

		startTime = System.currentTimeMillis();

		int resolved = 0;

		for (Coordinate coordinate : mergedCoordinates) {
			if (resolve(coordinate)) {
				resolved++;
			}
		}

		System.out.println("Skipped " + (mergedCoordinates.size() - resolved) + " coordinates, resolved " + resolved + " coordinates");
	}

	private static String _parseVersion(String line, String buildGradleContent) {
		int versionStartIndex = line.indexOf("version: \"");

		if (versionStartIndex == -1) {
			versionStartIndex = line.indexOf("version: ");

			if (versionStartIndex == -1) {
				return null;
			}

			versionStartIndex += "version: ".length();

			String versionVariable = line.substring(versionStartIndex, line.length()).trim();

			String versionDeclare = versionVariable + " = \"";

			versionStartIndex = buildGradleContent.indexOf(versionDeclare);

			if (versionStartIndex == -1) {
				return null;
			}

			versionStartIndex += versionDeclare.length();

			return buildGradleContent.substring(versionStartIndex, buildGradleContent.indexOf('"', versionStartIndex));
		}

		versionStartIndex += "version: \"".length();

		int versionEndIndex = line.indexOf('"', versionStartIndex);

		if (versionEndIndex == -1) {
			return null;
		}

		return line.substring(versionStartIndex, versionEndIndex);
	}
	private static List<Coordinate> _parseCoordinates(Path buildGradlePath)
		throws IOException {

		List<Coordinate> coordinates = new ArrayList<>();

		String buildGradleContent = new String(Files.readAllBytes(buildGradlePath));

		for (String line : StringUtil.split(buildGradleContent, '\n')) {
			line = line.trim();

			if (line.startsWith("parentThemes") || line.startsWith("match(")) {
				continue;
			}

			boolean test = line.startsWith("test");

			int groupStartIndex = line.indexOf("group: \"");

			if (groupStartIndex == -1) {
				continue;
			}

			groupStartIndex += "group: \"".length();

			int groupEndIndex = line.indexOf('"', groupStartIndex);

			if (groupEndIndex == -1) {
				continue;
			}

			String group = line.substring(groupStartIndex, groupEndIndex);

			int nameStartIndex = line.indexOf("name: \"");

			if (nameStartIndex == -1) {
				continue;
			}

			nameStartIndex += "name: \"".length();

			int nameEndIndex = line.indexOf('"', nameStartIndex);

			if (nameEndIndex == -1) {
				continue;
			}

			String name = line.substring(nameStartIndex, nameEndIndex);

			String version = _parseVersion(line, buildGradleContent);

			if (version == null) {
				continue;
			}

			// TODO Make sure these are added as a project dependency

			if (group.startsWith("com.liferay") && (version.equals("latest.release") || version.equals("default"))) {
				continue;
			}

			if (version.contains("@")) {
				continue;
			}

			if (version.endsWith("-LIFERAY-CACHED")) {
				continue;
			}

			Coordinate coordinate = new Coordinate(group, name, version, test);

			if (!_coordinateBlacklist.contains(coordinate)) {
				coordinates.add(coordinate);
			}
		}

		Collections.sort(coordinates);

		return coordinates;
	}

	public static void main(String[] args) throws IOException {
		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		buildCache(Paths.get("/home/trunks/git/liferay-portal"), buildProperties);
	}

	private static final Set<Coordinate> _coordinateBlacklist = new HashSet<>();

	private static final Set<Coordinate> _coordinateTestWhitelist = new HashSet<>();

	private static final String _SEARCH_URL =
		"https://search.maven.org/remotecontent?filepath=";

	private static final Logger _logger = Logger.getLogger(
		MavenUtil.class.getName());

}