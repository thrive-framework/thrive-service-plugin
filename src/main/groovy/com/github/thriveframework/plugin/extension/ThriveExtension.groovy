package com.github.thriveframework.plugin.extension

import org.gradle.api.Project
import org.gradle.api.provider.Property

class ThriveExtension {
    final Capabilities capabilities
    final Libraries libraries
    final Dockerfile dockerfile

    final Property<String> mainClassName

    final Property<Boolean> isRunnableProject

    ThriveExtension(Project project){
        capabilities = this.extensions.create("capabilities", Capabilities)
        libraries = this.extensions.create("libraries", Libraries)
        dockerfile = this.extensions.create("dockerfile", Dockerfile, project)

        mainClassName = project.objects.property(String)

        isRunnableProject = project.objects.property(Boolean)
        initDefaults()
    }

    private void initDefaults(){
        isRunnableProject.convention(true)
    }

    void service(boolean is = true){
        isRunnableProject.set is
    }

    void library(boolean is = true){
        service(!is)
    }
}
