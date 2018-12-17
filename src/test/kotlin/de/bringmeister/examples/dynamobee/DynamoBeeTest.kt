package de.bringmeister.examples.dynamobee

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dynamobee.Dynamobee
import com.github.dynamobee.changeset.ChangeLog
import com.github.dynamobee.changeset.ChangeSet
import de.bringmeister.infrastructure.docker.dynamoDbInDocker
import de.bringmeister.infrastructure.dynamodb.AsJson
import de.bringmeister.infrastructure.dynamodb.DynamoDbConfiguration
import de.bringmeister.infrastructure.dynamodb.DynamoDbTableUtil
import de.bringmeister.infrastructure.dynamodb.ObjectMapperHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.socialsignin.spring.data.dynamodb.core.DynamoDBTemplate
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        EntityDynamoDbRepository::class,
        DynamoBeeConfiguration::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class DynamoBeeTest {

    @Autowired
    private lateinit var repo: EntityDynamoDbRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @After
    fun before() {
        repo.deleteAll()
    }

    @Test
    fun `should save event`() {

        val expectedJson = """{"id":"42","data2":"some data 2","data3":"some data 1"}"""
        val expected = objectMapper.readValue(expectedJson, JsonNode::class.java)

        val payloadVersion2FromDynamoDb = repo.findById("42").get()

        assertThat(payloadVersion2FromDynamoDb.id).isEqualTo("42")
        assertThat(payloadVersion2FromDynamoDb.data).isEqualTo(expected)
    }
}

@Repository
@EnableScan
interface EntityDynamoDbRepository : CrudRepository<JsonBasedEntity, String>

@Configuration
class DynamoBeeConfiguration {

    @Bean
    fun dynamobee(amazonDynamoDB: AmazonDynamoDB): Dynamobee {
        return Dynamobee(amazonDynamoDB).apply {
            setChangeLogsScanPackage("de.bringmeister")
        }
    }
}

@ChangeLog
class DatabaseChangelog {

    @ChangeSet(order = "001", id = "createSchema", author = "Thomas")
    fun createSchema(amazonDynamoDB: AmazonDynamoDB) {

        // Step 1: Create an empty table in DynamoDB.

        DynamoDbTableUtil.createTableForEntity(JsonBasedEntity::class, amazonDynamoDB)
    }

    @ChangeSet(order = "002", id = "fillWithData", author = "Thomas")
    fun fillWithData(dynamoDBTemplate: DynamoDBTemplate) {

        // Step 2: Fill the table with some data (in an outdated format).

        val payloadVersion1 = PayloadVersion1(id = "42", data1 = "some data 1", data2 = "some data 2")
        val objectMapper = ObjectMapperHolder.getObjectMapper()
        val entity = JsonBasedEntity(id = "42", data = objectMapper.valueToTree(payloadVersion1))

        dynamoDBTemplate.save(entity)
    }

    @ChangeSet(order = "003", id = "migrateToNewVersion", author = "Thomas")
    fun migrateToNewVersion(dynamoDBTemplate: DynamoDBTemplate) {

        // Step 3: Iterate over the table and migrate every entity to the new version.

        val objectMapper = ObjectMapperHolder.getObjectMapper()
        dynamoDBTemplate.scan(JsonBasedEntity::class.java, DynamoDBScanExpression())
            .forEach {
                val oldVersion = objectMapper.treeToValue(it.data, PayloadVersion1::class.java)
                val newVersion = PayloadVersion2(id = oldVersion.id, data2 = oldVersion.data2, data3 = oldVersion.data1)
                val newJson = objectMapper.valueToTree<JsonNode>(newVersion)
                dynamoDBTemplate.save(it.copy(data = newJson))
            }
    }
}

@DynamoDBTable(tableName = "JsonBasedEntity")
data class JsonBasedEntity(

    @Id
    @get:DynamoDBHashKey(attributeName = "id")
    var id: String? = null,

    @DynamoDBAttribute(attributeName = "data")
    @AsJson
    var data: JsonNode? = null
)

// Version 1
data class PayloadVersion1(
    val id: String,
    val data1: String,
    val data2: String
    //val data3: String
)

// Version 2
data class PayloadVersion2(
    val id: String,
    //val data1: String,
    val data2: String,
    val data3: String
)
