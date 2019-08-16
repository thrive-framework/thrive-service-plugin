package com.github.thriveframework.plugin

import com.github.thriveframework.plugin.extension.ThriveExtension
import com.github.thriveframework.plugin.task.Echo
import com.github.thriveframework.plugin.task.WriteCapabilities
import com.github.thriveframework.plugin.task.WriteDockerfile
import com.github.thriveframework.plugin.utils.ThriveDirectories
import com.gorylenko.GitPropertiesPlugin
import groovy.util.logging.Slf4j
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin

import java.time.LocalDateTime
import java.time.ZoneId

import static com.github.thriveframework.plugin.utils.Projects.fullName

@Slf4j
class ThrivePlugin implements Plugin<Project> {

    private ThriveExtension extension
    private ThriveDirectories thriveDirectories

    @Override
    void apply(Project target) {
        thriveDirectories = new ThriveDirectories(target)
        verifyGradleVersion()

        configureExtensions(target)

        configureJavaLibrary(target)

        target.afterEvaluate { project ->
            configureRepositories(project)

            configureDirs(project)

            configureDependencies(project)

            configureGitProperties(project)

            //todo configure artifacts

            configureSpringBoot(project)

            configureProjectTasks(project)

            configureDockerTasks(project)
        }
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Thrive plugin requires Gradle 5.0 or later. The current version is "
                + GradleVersion.current());
        }
    }

    private void applyPluginIfNeeded(Project project, Class plugin){
        String pluginClassName = plugin.canonicalName
        log.info("Trying to apply plugin with implementation $pluginClassName to project ${fullName(project)}")
        if (!project.plugins.findPlugin(plugin)) {
            log.info("Applying $pluginClassName")
            project.apply plugin: plugin
        } else {
            log.info("$pluginClassName already applied")
        }
    }

    private void configureExtensions(Project project){
        log.info("Creating Thrive extenion in project ${fullName(project)}")
        this.extension = project.extensions.create("thrive", ThriveExtension, project)
    }

    private void configureJavaLibrary(Project project){
        applyPluginIfNeeded(project, JavaLibraryPlugin)
    }

    private void configureRepositories(Project project){
        log.info("Adding Maven Central, JFrog (snapshot and release), Spring milestone and JitPack repositories to project ${fullName(project)}")
        project.repositories {
            mavenCentral()
            //todo jcenter?
            maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local/' }
            maven { url 'http://oss.jfrog.org/artifactory/oss-release-local/' }
            maven { url 'https://repo.spring.io/milestone' }

            maven {
                name "jitpack"
                url "https://jitpack.io"
            }
        }
    }

    private void configureSpringBoot(Project project){
        if (extension.isRunnableProject.get()) {
            log.info "Configuring project ${fullName(project)} as runnable service"
            applyPluginIfNeeded(project, SpringBootPlugin)

            //fixme main class for booJar needs to ne given explicitly;
            // it can be resolved automatically for sure, Spring Cloud replaces Spring Boot
            // annotation somehow, so we should be able to replace it with Thrive annotation
            // as well

            project.springBoot {
                if (extension.mainClassName.isPresent()) {
                    mainClassName = extension.mainClassName.get()
                    log.info("Used ${extension.mainClassName.get()} as main class name")
                } else {
                    log.warn("Main class name not configured! This may break Spring Boot!")
                }
                buildInfo {
                    //todo it would be nice to inject git and build data to manifest as well
                    def tz = ZoneId.systemDefault();
                    def now = LocalDateTime.now().atZone(tz)
                    properties {
                        time = now.toInstant()
                        additional = [
                            timezone : tz.toString(),
                            timestamp: now.toEpochSecond()
                        ]
                    }
                }
            }

            project.bootRun {
                systemProperty "spring.profiles.active", "local"
            }

            project.ext {
                dockerized = true
                service = true
                library = false
            }
        } else {
            log.info("Configuring project ${fullName(project)} as a library")
            project.ext {
                dockerized = false
                service = false
                library = true
            }
        }
    }

    private void configureGitProperties(Project project){
        applyPluginIfNeeded(project, GitPropertiesPlugin)
    }

    private void configureDirs(Project project){
        log.info("Configuring Thrive resources directory, adding it to main source set in project ${fullName(project)}")
        project.ext {
            thriveDirectories = thriveDirectories
        }

        project.sourceSets {
            main {
                resources {
                    srcDir thriveDirectories.resources
                }
            }
        }
    }

    private void configureDependencies(Project project){
        if (extension.libraries.applyManagementPlugin.get()) {
            applyPluginIfNeeded(project, DependencyManagementPlugin)
        }

        if (extension.libraries.useThriveBom.get()) {
            def version = extension.libraries.bomVersion.get()
            log.info("Using Thrive BOM with version $version")
            project.dependencyManagement {
                imports {
                    mavenBom "com.github.thrive-framework:thrive-bom:${version}"
                }
            }
        }

        project.dependencies {
            //fixme these configs may not exist!
            if (extension.libraries.addLombok.get()) {
                log.info("Adding Lombok dependencies for ${fullName(project)}")
                compileOnly('org.projectlombok:lombok')
                annotationProcessor('org.projectlombok:lombok')
            }

            if (extension.libraries.addThriveCommon.get()) {
                log.info("Adding thrive-common dependency for ${fullName(project)}")
                implementation "com.github.thrive-framework.thrive:thrive-common"
            }
        }
    }

    private void configureProjectTasks(Project project){
        def group = "Thrive (common)"

        log.info("Creating 'writeVersion' task in project ${fullName(project)}")
        project.tasks.create(
            name: "writeVersion",
            type: Echo,
            description: "Writes project version to <buildDir>/thrive/metadata/version.txt (useful for build automation)",
            group: group
        ) {
            content = "${project.version}"
            target = new File(thriveDirectories.metadata, "version.txt")
        }

        project.build.dependsOn project.writeVersion

        log.info("Creating 'writeCapabilities' task in project ${fullName(project)}")
        project.tasks.create(
            name: "writeCapabilities",
            type: WriteCapabilities,
            description: "Writes properties describing Thrive capabilities to appropriate place",
            group: group
        ) {
            capabilities = extension.capabilities
            outputFile = new File(thriveDirectories.resources, "META-INF/capabilities.properties")
            comment = "Created by ${fullName(project)}:writeCapabilities on behalf of Thrive"
        }

        project.processResources.dependsOn project.writeCapabilities
    }

    private void configureDockerTasks(Project project){
        def group = "Thrive (Docker)"
        //todo log
        def dockerfileLocation = new File(project.projectDir, "Dockerfile")
        project.tasks.create(
            name: "writeDockerfile",
            type: WriteDockerfile,
            group: group,
            description: "Creates a Dockerfile suited for Thrive in main project directory (next to buildscript)"
        ){
            target = dockerfileLocation
            dockerfile = extension.dockerfile
        }

        if (project.ext.dockerized)
            project.build.dependsOn project.writeDockerfile
        project.clean.doLast {
            new File(project.projectDir, "Dockerfile").delete()
        }

        //docker build ?

        //docker compose todo add to extension
    }
}
