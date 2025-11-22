annotation.processing.enabled=true
annotation.processing.enabled.in.editor=false
annotation.processing.processors.list=
annotation.processing.run.all.processors=true
annotation.processing.source.output=${"$"}{build.generated.sources.dir}/ap-source-output
build.classes.dir=${"$"}{build.dir}/classes
build.classes.excludes=**/*.java,**/*.form
# This directory is removed when the project is cleaned:
build.dir=build
build.generated.dir=${"$"}{build.dir}/generated
build.generated.sources.dir=${"$"}{build.dir}/generated-sources
# Only compile against the classpath explicitly listed here:
build.sysclasspath=ignore
build.test.classes.dir=${"$"}{build.dir}/test/classes
build.test.results.dir=${"$"}{build.dir}/test/results
# Uncomment to specify the preferred debugger connection transport:
#debug.transport=dt_socket
debug.classpath=\
	${"$"}{run.classpath}
debug.test.classpath=\
	${"$"}{run.test.classpath}
# Files in build.classes.dir which should be excluded from distribution jar
dist.archive.excludes=
# This directory is removed when the project is cleaned:
dist.dir=dist
dist.javadoc.dir=${"$"}{dist.dir}/javadoc
endorsed.classpath=
excludes=
includes=**
jar.compress=false
# Space-separated list of extra javac options
javac.compilerargs=
javac.deprecation=false
javac.processorpath=\
	${"$"}{javac.classpath}
javac.source=21
javac.target=21
javac.test.classpath=\
	${"$"}{build.classes.dir}:\
	${"$"}{javac.classpath}
javac.test.processorpath=\
	${"$"}{javac.test.classpath}
javadoc.additionalparam=
javadoc.author=false
javadoc.encoding=${"$"}{source.encoding}
javadoc.noindex=false
javadoc.nonavbar=false
javadoc.notree=false
javadoc.private=false
javadoc.splitindex=true
javadoc.use=true
javadoc.version=false
javadoc.windowtitle=
main.class=
manifest.file=manifest.mf
meta.inf.dir=${"$"}{src.dir}/META-INF
mkdist.disabled=false
platform.active=default_platform

run.classpath=\
	${"$"}{javac.classpath}:\
	${"$"}{build.classes.dir}
# Space-separated list of JVM arguments used when running the project.
# You may also define separate properties like run-sys-prop.name=value instead of -Dname=value.
# To set system properties for unit tests define test-sys-prop.name=value:
run.jvmargs=
run.test.classpath=\
	${"$"}{javac.test.classpath}:\
	${"$"}{build.test.classes.dir}
source.encoding=UTF-8
excludes=**/*.css,**/*.js,**/*.json,**/*.sass,**/*.scss,**/*.class
file.reference.portal-web.src=${portalPath}/portal-web/docroot
src.portal-web.dir=${"$"}{file.reference.portal-web.src}
file.reference.portal-web-functional.src=${portalPath}/portal-web/test/functional
src.portal-web-functional.dir=${"$"}{file.reference.portal-web-functional.src}
file.reference.portal-definitions.src=${portalPath}/definitions
src.portal-definitions.dir=${"$"}{file.reference.portal-definitions.src}

<#if trunkPath??>
file.reference.tomcat-work.src=${trunkPath}/tomcat-${tomcatVersion}/work/Catalina/localhost/ROOT
src.tomcat-work.dir=${"$"}{file.reference.tomcat-work.src}
</#if>

<#list moduleNames as moduleName>
project.${moduleName}=${projectModulesPath}/${moduleName}
reference.${moduleName}.jar=${"$"}{project.${moduleName}}/dist/${moduleName}.jar
</#list>

javac.classpath=\
	<#list moduleNames as moduleName>
	${"$"}{reference.${moduleName}.jar}:\
	</#list>