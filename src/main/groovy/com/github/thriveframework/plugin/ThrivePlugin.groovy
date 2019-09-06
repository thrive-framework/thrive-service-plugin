package com.github.thriveframework.plugin

import com.github.thriveframework.plugin.extension.ThriveExtension
import com.github.thriveframework.plugin.task.WriteCapabilities
import com.github.thriveframework.plugin.task.WriteDockerfile
import com.github.thriveframework.plugin.utils.ThriveDirectories
import com.github.thriveframework.utils.plugin.Gradle
import com.gorylenko.GitPropertiesPlugin
import groovy.util.logging.Slf4j
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin

import java.time.LocalDateTime
import java.time.ZoneId

import static com.github.thriveframework.utils.plugin.task.VersionTasks.createPrintVersion
import static com.github.thriveframework.utils.plugin.task.VersionTasks.createWriteVersion
import static com.github.thriveframework.utils.plugin.Projects.createTask
import static com.github.thriveframework.utils.plugin.Projects.applyPlugin
import static com.github.thriveframework.utils.plugin.Projects.fullName

@Slf4j
class ThrivePlugin implements Plugin<Project> {

    private ThriveExtension extension
    private ThriveDirectories thriveDirectories

    @Override
    void apply(Project target) {
        thriveDirectories = new ThriveDirectories(target)
        Gradle.assertVersionAtLeast("5.0") //todo should we bump to 5.5?

        configureExtensions(target)

        configureJavaLibrary(target)

        target.afterEvaluate { project ->
            configureRepositories(project)

            configureDirs(project)

            configureDependencies(project)

            configureGitProperties(project)

            //todo configure artifacts

            configureSpringBoot(project)

            configureVersionTasks(project)

            configureProjectTasks(project)

            configureDockerTasks(project)
        }
    }

    private void configureExtensions(Project project){
        log.info("Creating Thrive extenion in project ${fullName(project)}")
        this.extension = project.extensions.create("thrive", ThriveExtension, project)
    }

    private void configureJavaLibrary(Project project){
        applyPlugin(project, JavaLibraryPlugin)
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
            applyPlugin(project, SpringBootPlugin)

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
        applyPlugin(project, GitPropertiesPlugin)
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
            applyPlugin(project, DependencyManagementPlugin)
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

    private void configureVersionTasks(Project project){
        createWriteVersion(project)
        createPrintVersion(project)
    }

    private void configureProjectTasks(Project project){
        def group = "thrive (common)"

        project.build.dependsOn project.writeVersion

        createTask(
            project,
            "writeCapabilities",
            WriteCapabilities,
            group,
            "Writes properties describing Thrive capabilities to appropriate place"
        ) {
            capabilities = extension.capabilities
            outputFile = new File(thriveDirectories.resources, "META-INF/capabilities.properties")
            comment = "Created by ${fullName(project)}:writeCapabilities on behalf of Thrive"
        }

        project.processResources.dependsOn project.writeCapabilities
    }

    private void configureDockerTasks(Project project){
        def group = "thrive (Docker)"

        def dockerfileLocation = new File(project.projectDir, "Dockerfile")

        project.afterEvaluate {
            if (extension.isRunnableProject.get()) {
                createTask(
                    project,
                    "writeDockerfile",
                    WriteDockerfile,
                    group,
                    "Creates a Dockerfile suited for Thrive in main project directory (next to buildscript)"
                ) {
                    target = dockerfileLocation
                    dockerfile = extension.dockerfile
                    dockerImage = extension.dockerImage
                }

                if (project.ext.dockerized)
                    project.build.dependsOn project.writeDockerfile
                project.clean.doLast {
                    dockerfileLocation.delete()
                }
            }
        }

        //docker build ; no more '?', its a definite "yep"

        //docker compose todo add to extension; in a moment, apply and preconfigure package plugin
    }
}
