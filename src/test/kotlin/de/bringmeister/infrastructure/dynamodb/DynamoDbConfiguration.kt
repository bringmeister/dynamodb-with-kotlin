package de.bringmeister.infrastructure.dynamodb

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions.EU_CENTRAL_1
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import de.bringmeister.connect.product.infrastructure.jackson.JacksonConfiguration
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableDynamoDBRepositories("de.bringmeister") // The root package!
@Import(value = [ObjectMapperHolder::class, JacksonConfiguration::class])
class DynamoDbConfiguration {

    /**
     * This is a local configuration in order to use DynamoDB in Docker. A production configuration,
     * for example for AWS ESC might look pretty simple like this:
     *
     *          fun dynamoDb(): AmazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient()
     *
     */
    @Bean
    fun amazonDynamoDB(@Value("\${aws.dynamodb.endpoint}") amazonDynamoDBEndpoint: String): AmazonDynamoDB {
        return AmazonDynamoDBClientBuilder
            .standard()
            .withCredentials(
                AWSStaticCredentialsProvider(
                    BasicAWSCredentials("key", "secret")
                )
            )
            .withEndpointConfiguration(
                EndpointConfiguration(amazonDynamoDBEndpoint, EU_CENTRAL_1.toString())
            )
            .build()
    }
}
