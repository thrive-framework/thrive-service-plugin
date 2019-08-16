package com.github.thriveframework.plugin.extension

import org.gradle.api.Project
import org.gradle.api.provider.Property

class ThriveExtension {
    final Capabilities capabilities
    final Libraries libraries
    final Dockerfile dockerfile

    final Property<String> mainClassName

    final Property<Boolean> useGitBasedVersioning

    final Property<Boolean> isRunnableProject

    ThriveExtension(Project project){
        capabilities = project.objects.newInstance(Capabilities)
        libraries = project.objects.newInstance(Libraries)
        dockerfile = project.objects.newInstance(Dockerfile, project)

        mainClassName = project.objects.property(String)

        useGitBasedVersioning = project.objects.property(Boolean)
        isRunnableProject = project.objects.property(Boolean)
        initDefaults()
    }

    private void initDefaults(){
        gitBasedVersioning()
        service()
    }

    void service(boolean is = true){
        isRunnableProject.set is
    }

    void library(boolean is = true){
        service(!is)
    }

    void gitBasedVersioning(boolean use = true){
        useGitBasedVersioning.set use
    }

    void libraries(Closure c){
        this.libraries.with c
    }

    void capabilities(Closure c){
        this.capabilities.with c
    }

    void dockerfile(Closure c){
        this.dockerfile.with c
    }
}
