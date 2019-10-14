package com.cognifide.gradle.aem.environment.reloader

import com.cognifide.gradle.aem.common.file.watcher.Event
import com.cognifide.gradle.aem.environment.docker.Container

class ContainerFileEvent(val container: Container, val event: Event)
