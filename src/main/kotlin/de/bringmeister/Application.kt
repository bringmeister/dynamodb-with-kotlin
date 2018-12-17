package de.bringmeister

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application

@Suppress("Detekt.SpreadOperator")
fun main(args: Array<String>) {
    // Ensure the JVM will refresh the cached IP values of AWS resources (e.g. service endpoints).
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")
    SpringApplication.run(Application::class.java, *args)
}
