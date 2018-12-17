package de.bringmeister.examples.simple

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import de.bringmeister.infrastructure.docker.dynamoDbInDocker
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
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        DynamoDbTableUtil::class,
        SimpleEntityDynamoDbRepository::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class SimpleEntityTest {

    @Autowired
    private lateinit var repo: SimpleEntityDynamoDbRepository

    @Autowired
    private lateinit var util: DynamoDbTableUtil

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @Before
    fun before() {
        util.createTableForEntity(SimpleEntity::class)
        repo.deleteAll()
    }

    @Test
    fun `should save and ready simple entity from DynamoDB`() {
        val simpleEntity = SimpleEntity(id = "42", firstname = "Jon", lastname = "Doe")
        repo.save(simpleEntity)
        val simpleEntityFromDynamoDb = repo.findById("42") // Returns optional!
        assertThat(simpleEntity).isEqualTo(simpleEntityFromDynamoDb.get())
    }
}

@Repository
@EnableScan
interface SimpleEntityDynamoDbRepository : CrudRepository<SimpleEntity, String>

@DynamoDBTable(tableName = "SimpleEntity")
data class SimpleEntity(

    @Id
    @get:DynamoDBHashKey
    var id: String? = null,

    @DynamoDBAttribute
    var firstname: String? = null,

    @DynamoDBAttribute
    var lastname: String? = null
)
