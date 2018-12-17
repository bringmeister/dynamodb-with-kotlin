package de.bringmeister.examples.jsonmigration

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.bringmeister.infrastructure.docker.dynamoDbInDocker
import de.bringmeister.infrastructure.dynamodb.AsJson
import de.bringmeister.infrastructure.dynamodb.DynamoDbConfiguration
import de.bringmeister.infrastructure.dynamodb.DynamoDbTableUtil
import de.bringmeister.infrastructure.dynamodb.ObjectMapperHolder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.test.context.junit4.SpringRunner
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        JsonBasedEntityDynamoDbRepository::class,
        Version1Adapter::class,
        Version2Adapter::class,
        DynamoDbTableUtil::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class MigrationIntegrationTest {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var util: DynamoDbTableUtil

    @Autowired
    private lateinit var version1Adapter: Version1Adapter

    @Autowired
    private lateinit var version2Adapter: Version2Adapter

    @Autowired
    private lateinit var repo: JsonBasedEntityDynamoDbRepository

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @Before
    fun before() {
        util.createTableForEntity(JsonBasedEntity::class)
        repo.deleteAll()
    }

    @Test
    fun `should save event`() {

        // Step 1: We create an object and save it to our database.
        //         This object is called version 1. We read it again
        //         afterwards and assert that we still have the same
        //         data. Easy.

        val payloadVersion1 = PayloadVersion1(
            id = "42",
            data1 = "some data 1",
            data2 = "some data 2"
        )

        version1Adapter.save(payloadVersion1)

        val payloadVersion1FromDynamoDb = version1Adapter.get("42")

        assertThat(payloadVersion1FromDynamoDb).isEqualTo(payloadVersion1)

        // Step 2: We load the same object as another version. This means
        //         instead of converting the stored JSON to PayloadVersion1.kt
        //         we convert it to PayloadVersion2.kt. Both objects are
        //         slightly different. But since we wrote a migration we are
        //         able to read the old format (version 1) as a new object
        //         (version 2).

        val payloadVersion2FromDynamoDb = version2Adapter.get("42")

        assertThat(payloadVersion2FromDynamoDb.id).isEqualTo(payloadVersion1.id)
        assertThat(payloadVersion2FromDynamoDb.data2).isEqualTo(payloadVersion1.data2)
        assertThat(payloadVersion2FromDynamoDb.data3).isEqualTo(payloadVersion1.data1) // Field names have changed!

        // Step 3: In step 2 we loaded the old data in a new format. Now we really
        //         want to do a migration. To do so, we load all entities in the old
        //         version 1, migrate them and write them back to DynamoDB. Afterwards
        //         all entities will have the new version 2!

        assertThat(repo.findByVersion("1")).hasSize(1)
        assertThat(repo.findByVersion("2")).hasSize(0)

        val migration = Version1ToVersion2Migration()
        repo
            .findByVersion("1")
            .forEach {
                val newJson = migration.migrate(it.data!!)
                repo.save(it.copy(version = "2", data = newJson))
            }

        assertThat(repo.findByVersion("1")).hasSize(0)
        assertThat(repo.findByVersion("2")).hasSize(1)

        // Step 4: Reading all data in the new version is easy now. There's
        //         nothing to do.

        val afterMigration = version2Adapter.get("42")

        assertThat(afterMigration.id).isEqualTo(payloadVersion1.id)
        assertThat(afterMigration.data2).isEqualTo(payloadVersion1.data2)
        assertThat(afterMigration.data3).isEqualTo(payloadVersion1.data1) // Field names have changed!

        // Step 5: However, we cannot read the data in the old version.
        //         There exists no migration back from version 2 to the old
        //         version 1. So we will run into an exception if we try
        //         to do so.

        try {
            version1Adapter.get("42")
            fail("Should throw exception!")
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Cannot read entity - wrong version! [expected=1, actual=2]")
        }
    }
}

annotation class Version(val version: String)
annotation class Migrations(val migrations: Array<KClass<out Migration>>)

interface Migration {
    fun versionToApplyTo(): String
    fun targetVersion(): String
    fun migrate(jsonNode: JsonNode): JsonNode
}

class Version1ToVersion2Migration : Migration {

    override fun migrate(jsonNode: JsonNode): JsonNode {
        val oldVersion = ObjectMapperHolder.getObjectMapper().treeToValue(jsonNode, PayloadVersion1::class.java)
        val newVersion =
            PayloadVersion2(
                id = oldVersion.id,
                data2 = oldVersion.data2,
                data3 = oldVersion.data1
            )
        return ObjectMapperHolder.getObjectMapper().valueToTree(newVersion)
    }

    override fun targetVersion(): String {
        return "2"
    }

    override fun versionToApplyTo(): String {
        return "1"
    }
}

@Repository
@EnableScan
interface JsonBasedEntityDynamoDbRepository : CrudRepository<JsonBasedEntity, String> {
    fun findByVersion(version: String): List<JsonBasedEntity>
}

@Service
class Version1Adapter(
    private val repository: JsonBasedEntityDynamoDbRepository,
    private val objectMapper: ObjectMapper
) {

    private val targetVersion = PayloadVersion1::class.java.getAnnotation(Version::class.java).version
    private val migrations = PayloadVersion1::class.java.getAnnotation(Migrations::class.java).migrations

    fun save(payload: PayloadVersion1) {
        val entity = JsonBasedEntity(payload.id, targetVersion, objectMapper.valueToTree(payload))
        repository.save(entity)
    }

    fun get(sku: String): PayloadVersion1 {
        val entity = repository.findById(sku).get()
        return asVersion(objectMapper, entity, targetVersion, PayloadVersion1::class.java, migrations)
    }
}

@Service
class Version2Adapter(
    private val repository: JsonBasedEntityDynamoDbRepository,
    private val objectMapper: ObjectMapper
) {

    private val targetVersion = PayloadVersion2::class.java.getAnnotation(Version::class.java).version
    private val migrations = PayloadVersion2::class.java.getAnnotation(Migrations::class.java).migrations

    fun save(payload: PayloadVersion2) {
        val entity = JsonBasedEntity(payload.id, targetVersion, objectMapper.valueToTree(payload))
        repository.save(entity)
    }

    fun get(sku: String): PayloadVersion2 {
        val entity = repository.findById(sku).get()
        return asVersion(objectMapper, entity, targetVersion, PayloadVersion2::class.java, migrations)
    }
}

private fun <T> asVersion(
    objectMapper: ObjectMapper,
    entity: JsonBasedEntity,
    version: String,
    clazz: Class<T>,
    migrations: Array<KClass<out Migration>>
): T {

    val jsonNode = entity.data!!
    if (entity.version != version) {
        for (migration in migrations) {
            val migrator = migration.createInstance()
            if (migrator.versionToApplyTo() == entity.version) {
                val json = migrator.migrate(jsonNode)
                return objectMapper.treeToValue(json, clazz)
            }
        }
        throw RuntimeException("Cannot read entity - wrong version! [expected=$version, actual=${entity.version}]")
    } else {
        return objectMapper.treeToValue(jsonNode, clazz)
    }
}

@DynamoDBTable(tableName = "JsonBasedEntity")
data class JsonBasedEntity(

    @Id
    @get:DynamoDBHashKey(attributeName = "id")
    var id: String? = null,

    @DynamoDBIndexHashKey(attributeName = "version", globalSecondaryIndexName = "index_version")
    var version: String? = null,

    @DynamoDBAttribute(attributeName = "data")
    @AsJson
    var data: JsonNode? = null
)

@Version("1")
@Migrations([])
data class PayloadVersion1(
    val id: String,
    val data1: String,
    val data2: String
    //val data3: String
)

@Version("2")
@Migrations([Version1ToVersion2Migration::class])
data class PayloadVersion2(
    val id: String,
    //val data1: String,
    val data2: String,
    val data3: String
)
