package com.github.thriveframework.plugin.extension

import groovy.transform.ToString
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

@ToString
class DockerImage {
    final Property<String> dockerRepositoryPrefix
    final Property<String> dockerRepository
    final Property<String> name
    final Property<String> tag
    final Property<String> fullRepositoryName
    final Property<String> localImageName
    final Property<String> imageName

    @Inject
    DockerImage(Project project){
        dockerRepositoryPrefix = project.objects.property(String)
        dockerRepository = project.objects.property(String)
        name = project.objects.property(String)
        tag = project.objects.property(String)
        fullRepositoryName = project.objects.property(String)
        localImageName = project.objects.property(String)
        imageName = project.objects.property(String)
        initDefaults(project)
    }

    private void initDefaults(Project project){
        dockerRepositoryPrefix.convention(project.provider({ "" }))
        dockerRepository.convention project.provider { "${project.group}".split("[.]").last() }
        name.convention "${project.name}"
        tag.convention project.provider { "${project.version}" }
        fullRepositoryName.convention dockerRepositoryPrefix.map { prefix ->
            dockerRepository.map { repo ->
                "${prefix}${repo}"
            }.get()
        }
        localImageName.convention name.map { n ->
            tag.map { t ->
                //todo empty tag?
                "$n:$t"
            }.get()
        }
        imageName.convention fullRepositoryName.map { repo ->
            def prefixedRepo = "$repo"
            if (prefixedRepo)
                prefixedRepo += "/"
            localImageName.map { name ->
                "${prefixedRepo}${name}"
            }.get()
        }
    }
}
