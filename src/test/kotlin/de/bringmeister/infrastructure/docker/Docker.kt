package de.bringmeister.infrastructure.docker

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports

fun dynamoDbInDocker() = KGenericContainer("richnorth/dynalite:latest")
    .withCreateContainerCmdModifier {
        it.withPortBindings(
            Ports(
                PortBinding(
                    Ports.Binding("localhost", "15378"),
                    ExposedPort.tcp(4567)
                )
            )
        )
    }
