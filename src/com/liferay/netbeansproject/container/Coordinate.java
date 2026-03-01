/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.netbeansproject.container;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Shuyang Zhou
 */
public class Coordinate implements Comparable<Coordinate> {

	public Coordinate(String line) {
		String[] parts = line.split(":");

		_groupId = parts[0];
		_artifactId = parts[1];
		_version = parts[2];
		_test = false;

		_filePath = _toFilePath(_groupId, _artifactId, _version);
	}


	public Coordinate(
		String groupId, String artifactId, String version, boolean test) {

		_groupId = groupId;
		_artifactId = artifactId;
		_version = version;
		_test = test;

		_filePath = _toFilePath(groupId, artifactId, version);
	}

	public Dependency toDependency(Path basePath) {
		return toDependency(basePath, _test);
	}

	public Dependency toDependency(Path basePath, boolean test) {
		Path sourcePath = basePath.resolve(_filePath.replace(".jar", "-sources.jar"));

		if (Files.notExists(sourcePath)) {
			sourcePath = null;
		}
		else {
			sourcePath = sourcePath.toAbsolutePath();
		}

		return new Dependency(basePath.resolve(_filePath).toAbsolutePath(), sourcePath, test);
	}

	private static String _toFilePath(String groupId, String artifactId, String version) {
		StringBuilder sb = new StringBuilder();

		sb.append(groupId.replace('.', '/'));
		sb.append('/');
		sb.append(artifactId);
		sb.append('/');
		sb.append(version);
		sb.append('/');
		sb.append(artifactId);
		sb.append('-');
		sb.append(version);
		sb.append(".jar");

		return sb.toString();
	}
	public String getFilePath() {
		return _filePath;
	}

	public String getGroupId() {
		return _groupId;
	}

	public String getArtifactId() {
		return _artifactId;
	}

	public String getVersion() {
		return _version;
	}

	public boolean isTest() {
		return _test;
	}

	public void setTest(boolean test) {
		_test = test;
	}

	private final String _groupId;
	private final String _artifactId;
	private final String _version;
	private boolean _test;
	private final String _filePath;

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + Objects.hashCode(this._groupId);
		hash = 67 * hash + Objects.hashCode(this._artifactId);
		hash = 67 * hash + Objects.hashCode(this._version);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Coordinate other = (Coordinate)obj;
		if (!Objects.equals(this._groupId, other._groupId)) {
			return false;
		}
		if (!Objects.equals(this._artifactId, other._artifactId)) {
			return false;
		}
		return Objects.equals(this._version, other._version);
	}


	@Override
	public int compareTo(Coordinate coordinate) {
		int result = _groupId.compareTo(coordinate._groupId);

		if (result != 0) {
			return result;
		}

		result = _artifactId.compareTo(coordinate._artifactId);

		if (result != 0) {
			return result;
		}

		return _version.compareTo(coordinate._version);
	}

	@Override
	public String toString() {
		return _groupId + ":" + _artifactId + ":" + _version;
	}

}