package de.bringmeister.infrastructure.dynamodb

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * This is a workaround for our @AsJson annotation. This annotation needs an
 * instance of the configured object mapper from the Spring context to serialize
 * and deserialize objects to JSON and vice versa. However, an annotation cannot
 * be managed by Spring and cannot autowire any dependency. We use this class to
 * get access to the object mapper by using a static reference.
 *
 * @see AsJson
 */
@Service
class ObjectMapperHolder(private val objectMapper: ObjectMapper) {

    companion object {

        private var objectMapper: ObjectMapper? = null

        fun getObjectMapper(): ObjectMapper {
            return objectMapper
                ?: throw NullPointerException("ObjectMapper not set - is the Spring context already up?")
        }
    }

    @PostConstruct
    fun useObjectMappper() {
        ObjectMapperHolder.objectMapper = objectMapper
    }
}
