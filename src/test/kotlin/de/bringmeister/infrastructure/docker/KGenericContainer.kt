package de.bringmeister.infrastructure.docker

import org.testcontainers.containers.GenericContainer

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
