package com.liferay.netbeansproject.gradle;

import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleResolver {

	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		String moduleList = arguments.get("module.list");

		if (moduleList == null) {
			throw new IllegalArgumentException("Missing module.list");
		}

		String defaultGradleContent = new String(
			Files.readAllBytes(Paths.get("../common/default.gradle")));

		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path moduleProjectsDirPath = Paths.get(
			properties.getProperty("project.dir"), "modules");

		StringBuilder sb = new StringBuilder();

		for(String module : StringUtil.split(moduleList, ',')) {
			Path modulePath = Paths.get(module);

			Path moduleName = modulePath.getFileName();

			Path moduleProjectPath = moduleProjectsDirPath.resolve(moduleName);

			_createGradleFile(
				defaultGradleContent, _extractDependency(modulePath),
				moduleProjectPath.resolve("build.gradle"));

			sb.append("include \"");
			sb.append(moduleName);
			sb.append("\"\n");
		}

		Files.write(
			moduleProjectsDirPath.resolve("settings.gradle"), Arrays.asList(sb),
			Charset.defaultCharset());
	}

	private static String _extractDependency(Path modulePath)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return "";
		}

		String content = new String(Files.readAllBytes(buildGradlePath));

		Matcher jenkinsMatcher = _jenkinsPattern.matcher(content);

		StringBuilder sb = new StringBuilder();

		while(jenkinsMatcher.find()) {
			sb.append(jenkinsMatcher.group(0));
			sb.append('\n');
		}

		Matcher dependencyMatcher = _dependencyPattern.matcher(content);

		while(dependencyMatcher.find()) {
			sb.append(dependencyMatcher.group(0));
			sb.append('\n');
		}

		return sb.toString();
	}

	private static void _createGradleFile(
			String defaultGradleContent, String dependency, Path gradleFilePath)
		throws IOException {

		Matcher projectMatcher = _projectPattern.matcher(dependency);

		dependency = projectMatcher.replaceAll("");

		Matcher unusedDependencyMatcher =
			_unusedDependencyPattern.matcher(dependency);

		dependency = unusedDependencyMatcher.replaceAll("");

		Matcher portalMatcher = _portalPattern.matcher(dependency);

		defaultGradleContent =
			StringUtil.replace(
				defaultGradleContent, "*insert-filepath*",
				"\"" + gradleFilePath.getParent() + "/dependency.properties\"");

		String gradleContent = StringUtil.replace(
			defaultGradleContent, "*insert-dependencies*",
			_replaceKeywords(portalMatcher.replaceAll("")));

		Files.write(
			gradleFilePath, Arrays.asList(gradleContent),
			Charset.defaultCharset());
	}

	private static String _replaceKeywords(String dependency) {
		dependency = StringUtil.replace(dependency, "optional, ", "");
		dependency = StringUtil.replace(
			dependency, "antlr group", "compile group");
		dependency = StringUtil.replace(
			dependency, "jarjar group", "compile group");
		dependency = StringUtil.replace(
			dependency, "jruby group", "compile group");
		dependency = StringUtil.replace(
			dependency, "jnaerator classifier: \"shaded\",", "compile");
		dependency = StringUtil.replace(dependency, "provided", "compile");
		dependency = StringUtil.replace(
			dependency, "testIntegrationCompile", "testCompile");

		return  StringUtil.replace(
			dependency, "testCompile", "testConfiguration");
	}

	private static final Pattern _dependencyPattern =
		Pattern.compile("dependencies(\\s*)\\{[^}]*}");

	private static final Pattern _jenkinsPattern =
		Pattern.compile("String jenkins.*");

	private static final Pattern _portalPattern =
		Pattern.compile(
			"\t(compile|provided|testCompile|testIntegrationCompile)\\s*group:"
				+ "\\s\"com\\.liferay\\.portal\".*\\n");

	private static final Pattern _projectPattern =
		Pattern.compile(
			"\t(compile|provided|testCompile|testIntegrationCompile|"
				+ "frontendThemes)\\s*project.*\\n");

	private static final Pattern _unusedDependencyPattern =
		Pattern.compile("\tconfigAdmin\\s*group.*\\n");
}