package de.bringmeister.examples.json

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import de.bringmeister.infrastructure.docker.dynamoDbInDocker
import de.bringmeister.infrastructure.dynamodb.AsJson
import de.bringmeister.infrastructure.dynamodb.DynamoDbConfiguration
import de.bringmeister.infrastructure.dynamodb.DynamoDbTableUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        DynamoDbTableUtil::class,
        PersonEntityDynamoDbRepository::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class MigrationIntegrationTest {

    @Autowired
    private lateinit var repo: PersonEntityDynamoDbRepository

    @Autowired
    private lateinit var util: DynamoDbTableUtil

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @Before
    fun before() {
        util.createTableForEntity(PersonEntity::class)
        repo.deleteAll()
    }

    @Test
    fun `should save object as JSON in DynamoDB`() {

        val person = Person("Jon", "Doe")
        val personEntity = PersonEntity(id = "p-001", data = person)

        repo.save(personEntity)

        val personEntityFromDynamoDb = repo.findById("p-001") // Returns optional!
        assertThat(personEntity).isEqualTo(personEntityFromDynamoDb.get())
    }
}

@Repository
@EnableScan
interface PersonEntityDynamoDbRepository : CrudRepository<PersonEntity, String>

data class Person(
    val firstname: String,
    val lastname: String
)

@DynamoDBTable(tableName = "PersonEntity")
data class PersonEntity(

    @DynamoDBHashKey(attributeName = "id")
    var id: String? = null,

    @DynamoDBAttribute(attributeName = "data")
    @AsJson // Custom annotation to save the object as plain JSON!
    var data: Person? = null
)
