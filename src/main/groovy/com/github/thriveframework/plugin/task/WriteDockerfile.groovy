package com.github.thriveframework.plugin.task

import com.github.thriveframework.plugin.extension.Dockerfile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

@CacheableTask
class WriteDockerfile extends Echo {
    final Property<Dockerfile> dockerfile

    @Inject
    WriteDockerfile(ObjectFactory objects) {
        super(objects)
        dockerfile = objects.property(Dockerfile)
        content.set(prepareContent())
    }

    private Provider<String> prepareContent(){
        /*
         * Ordering of commands is based on what will change the least frequently.
         * I've assumed that workdir and project name are basically constant; exposed ports will
         * change least frequently, labels may change a bit more often (because of version change),
         * entrypoint as well (same, because of versioning), and each COPY may be different
         * (as it will probably change per-commit).
         */

        dockerfile.map({config ->
            """FROM ${config.baseImage.get()}
LABEL name="${config.name.get()}"

WORKDIR ${config.workdir.get()}

${config.exposes.get().collect {
    "EXPOSE $it"
}.join("\n")}

${config.maintainer.isPresent() ? "LABEL maintainer=\"${config.maintainer.get()}\"" : ""}
LABEL version="${config.version.get()}"
${config.labels.get().isEmpty() ? "" : "LABEL ${config.labels.get().collect {k, v -> "$k=\"$v\""}.sort().join(",")}"}

ENTRYPOINT ["java","-jar","${config.workdir.get()}/${config.name.get()}-${config.version.get()}-boot.jar"]

COPY ./build/libs/${config.name.get()}-${config.version.get()}.jar ./${config.name.get()}-${config.version.get()}-boot.jar
""" //todo: JAVA_OPTS
        })
    }

    //todo I think this can be safely removed
    @TaskAction
    void run(){
        super.run()
    }
}
