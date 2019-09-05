package com.github.thriveframework.plugin.task

import com.github.thriveframework.plugin.extension.Capabilities
import groovy.util.logging.Slf4j
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties

import javax.inject.Inject


@CacheableTask
@Slf4j
class WriteCapabilities extends WriteProperties {
    final Property<Capabilities> capabilities

    @Inject
    WriteCapabilities(ObjectFactory objects){
        capabilities = objects.property(Capabilities)
    }

    @TaskAction
    void run(){
        def map = capabilities.get().asMap()
        log.info("Capabilities map: ${map}")

        def props = [:]
        //used to log "no capabilites" only once
        boolean noCapabilities = false
        map.each { capability, value ->
            try {
                if (value) {
                    [
                        "thrive.capability." + capability,
                        "info.tags.capability." + capability,
                        "spring.cloud.zookeeper.discovery.metadata.capability." + capability
                    ].each { propKey ->
                        props.put(propKey, value as String)
                    }
                }
            } catch (MissingPropertyException e) {
                if (!noCapabilities) {
                    log.warn "No capabilities defined!"
                    noCapabilities = true
                }
            }
        }
        this.properties = props
        log.info("Properties to be written: $props")
        writeProperties() //fixme this is annotated with TaskAction as well; I think it may be called twice
    }
}
