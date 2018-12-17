package de.bringmeister.examples.compositekey

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
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
        SongEntityDynamoDbRepository::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class CompositeKeyTest {

    @Autowired
    private lateinit var repo: SongEntityDynamoDbRepository

    @Autowired
    private lateinit var util: DynamoDbTableUtil

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @Before
    fun before() {
        util.createTableForEntity(SongEntity::class)
        repo.deleteAll()
    }

    @Test
    fun `should save and ready composite key entity from DynamoDB`() {
        val song = SongEntity("New York", "Trilogy", "Frank Sinatra", "1980")
        repo.save(song)
        val songFromDynamoDb = repo.findById(SongId("New York", "Trilogy")) // Returns optional!
        assertThat(song).isEqualTo(songFromDynamoDb.get())
    }

    @Test
    fun `should save songs with the same title but different album`() {

        val song1 = SongEntity("New York", "Trilogy", "Frank Sinatra", "1980")
        val song2 = SongEntity("New York", "The Essential Ray Conniff", "Ray Conniff", "1989")

        repo.save(song1)
        repo.save(song2)

        val song1FromDynamoDb = repo.findById(SongId("New York", "Trilogy")) // Returns optional!
        assertThat(song1).isEqualTo(song1FromDynamoDb.get())

        val song2FromDynamoDb = repo.findById(SongId("New York", "The Essential Ray Conniff")) // Returns optional!
        assertThat(song2).isEqualTo(song2FromDynamoDb.get())

        val songsFromDynamoDb = repo.findAllBySongName("New York")
        assertThat(songsFromDynamoDb).hasSize(2)
    }
}

@Repository
@EnableScan
interface SongEntityDynamoDbRepository : CrudRepository<SongEntity, SongId> {
    fun findAllBySongName(songName: String): List<SongEntity>
}

@DynamoDBTable(tableName = "SongEntity")
data class SongEntity(

    @get:DynamoDBHashKey(attributeName = "songName") // annotation only on the getter! -> "@get:"
    var songName: String? = null,

    @get:DynamoDBRangeKey(attributeName = "albumName") // annotation only on the getter! -> "@get:"
    var albumName: String? = null,

    @DynamoDBAttribute(attributeName = "artist")
    var artist: String? = null,

    @DynamoDBAttribute(attributeName = "year")
    var year: String? = null

) {

    // Nice hack to provide the mandatory ID for Spring Data!
    @Id
    private var id: SongId? = null
        get() {
            return SongId(songName, albumName)
        }
}

data class SongId(
    @DynamoDBHashKey
    var songName: String? = null,
    @DynamoDBRangeKey
    var albumName: String? = null
)
