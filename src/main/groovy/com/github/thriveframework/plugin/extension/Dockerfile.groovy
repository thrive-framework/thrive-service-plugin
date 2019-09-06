package com.github.thriveframework.plugin.extension

import com.github.thriveframework.plugin.utils.Defaults
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

class Dockerfile {
    final Property<String> baseImage
    final ListProperty<String> exposes

    final Property<String> maintainer
    final Property<String> version
    final Property<String> artifactVersion
    //todo add artifactname with default on image name
    final MapProperty<String, String> labels
    final Property<String> workdir

    @Inject
    Dockerfile(Project project){ //we need to retrieve project props, so we inject it instead of factory
        ObjectFactory objects = project.objects
        baseImage = objects.property(String)
        exposes = objects.listProperty(String)
        maintainer = objects.property(String)
        version = objects.property(String)
        artifactVersion = objects.property(String)
        labels = objects.mapProperty(String, String)
        workdir = objects.property(String)
        initDefaults(project)
    }

    private void initDefaults(Project project){
        ProviderFactory providers = project.providers
        baseImage.convention Defaults.baseImage
        exposes.convention(["8080"])
        //todo should be in higher level defaults, like ThriveExtension, same with artifactName
        version.convention providers.provider({ "${project.extensions.findByType(ThriveExtension).dockerImage.tag.get()}" })
        artifactVersion.convention version
        labels.convention([:])
        workdir.convention("/var/opt/${project.name}")
    }

    //todo add fluent setters
}
