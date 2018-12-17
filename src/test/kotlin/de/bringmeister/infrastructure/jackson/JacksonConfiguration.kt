package de.bringmeister.connect.product.infrastructure.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(JacksonAutoConfiguration::class)
class JacksonConfiguration {

    @Bean
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder
                .featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .modulesToInstall(
                    KotlinModule(),
                    JavaTimeModule(),
                    Jdk8Module()
                )
        }
    }
}
