# thrive-service-plugin

This plugin automates a lot in scope of [Thrive](https://github.com/thrive-framework).

Basically, what you get is (some of those are optional, see Usage):

- applied [`java-library`](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin
- configured common Maven repositories (Maven Central, JFrog snapshots and releases, Spring milestones, Jitpack)
- applied [git properties](https://github.com/n0mer/gradle-git-properties) plugin
- applied [dependency management](https://github.com/spring-gradle-plugins/dependency-management-plugin) plugin
- used [Thrive BOM](https://github.com/thrive-framework/thrive-bom)
- configured [Lombok](https://projectlombok.org/) dependency
- configured [`thrive-common`](https://github.com/thrive-framework/thrive/tree/0.3.0/thrive-common) dependencies
- applied and preconfigured (specifically for Thrive) [Spring Boot](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/) plugin
- added `printVersion` and `addVersion` tasks
- added `writeDockerfile` task
- configured Thrive internals (thrive-related build directories, `writeCapabilities` task)

Basically, you should be able to apply this plugin and write your business microservice, adding only the dependencies
that you need (and not those that Thrive needs, as it's done for you). Once you have your business logic as a code,
you should be able to deploy this service with [thrive-package-plugin](https://github.com/thrive-framework/thrive-package-plugin).

> Keep in mind, that this plugin does NOT apply [versioning plugin](https://github.com/thrive-framework/thrive-versioning-plugin). 
> If you're releasing your code with JitPack, I strongly recommend that you use it.

## Get it

> This plugin is already usable, even though there is still some work to do. Current work happens mainly in scope
> of package plugin, once it's done, there will be some tweaking here.
>
> Long story short, this should already provide you with added value.

Use JitPack and declare proper buildscript dependency:

    buildscript {
        repositories {
            mavenCentral()
            maven {
                name "jitpack"
                url "https://jitpack.io"
            }
            gradlePluginPortal()
        }
        dependencies {
            classpath "com.github.thrive-framework:thrive-service-plugin:0.3.0-SNAPSHOT"
        }
    }

> Don't forget to use Maven Central and Plugin Portal repositories - some dependencies of this project are hosted
> there.

Then apply it:

    apply plugin: "com.github.thrive"

> New-style plugin syntax may come soon. I have other priorities right now.

## Usage

> todo document extension layout and what does it do
>
> You can pretty much figure it out yourself by browsing [this package](/src/main/groovy/com/github/thriveframework/plugin/extension)
> though.

## ToDo

- finish the implementation (duh)
  - apply package plugin
  - preconfigure it
  - add tasks for docker build (?)
  - ... ?
  - REFACTOR!!!
    - sanitize extensions (use conventions instead of setting values, use extensions' extensions instead of method
    accepting closures)
- test it somehow
- provie CircleCI config