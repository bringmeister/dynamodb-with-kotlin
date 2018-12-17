package de.bringmeister.infrastructure.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped
import com.fasterxml.jackson.databind.JsonMappingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * This is a custom version of Spring Data's @DynamoDBTypeConvertedJson annotation written in
 * Kotlin. It provides a way to use the object mapper configured in the Spring context to serialize
 * and deserialize objects to JSON and vice versa. Inject a custom object mapper isn't possible when
 * using Spring Data's @DynamoDBTypeConvertedJson annotation and is the main reason for implementing
 * this class. We need to use a custom object mapper as we need to register some modules in order to
 * handle Kotlin, money types, dates... and so on.
 *
 * @see de.bringmeister.connect.product.infrastructure.jackson.JacksonConfiguration
 * @see com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedJson
 */
@DynamoDBTypeConverted(converter = Converter::class)
@DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class AsJson(val targetType: KClass<out Any> = Unit::class)

class Converter<T : Any>(targetType: Class<T>, annotation: AsJson) : DynamoDBTypeConverter<String, T> {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    private val targetType: KClass<T> =
        if (annotation.targetType == Unit::class) targetType.kotlin else annotation.targetType as KClass<T>

    override fun convert(obj: T): String {
        return ObjectMapperHolder.getObjectMapper().writeValueAsString(obj)
    }

    override fun unconvert(json: String): T? {
        return try {
            ObjectMapperHolder.getObjectMapper().readValue(json, targetType.java)
        } catch (e: JsonMappingException) {
            log.error("Cannot read entity from DynamoDB - deserialization failed!", e)
            null
        }
    }
}
