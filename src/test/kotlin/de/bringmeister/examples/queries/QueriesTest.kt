package de.bringmeister.examples.queries

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey
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
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        DynamoDbTableUtil::class,
        QueriesDynamoDbRepository::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class QueriesTest {

    @Autowired
    private lateinit var repo: QueriesDynamoDbRepository

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
    fun `should query songs`() {

        val song1 = SongEntity("s-001", "New York", "Trilogy", "Frank Sinatra", "1980")
        val song2 = SongEntity("s-002", "New York", "The Essential Ray Conniff", "Ray Conniff", "1989")
        val song3 = SongEntity("s-003", "But Not for Me", "Trilogy", "Frank Sinatra", "1980")

        repo.save(song1)
        repo.save(song2)
        repo.save(song3)

        assertThat(repo.findAllBySongName("New York")).hasSize(2)
        assertThat(repo.findAllByAlbumName("Trilogy")).hasSize(2)
        assertThat(repo.findAllByAlbumName("Unknown album")).hasSize(0)
        assertThat(repo.findById("s-002")).isPresent
        assertThat(repo.findById("s-007")).isNotPresent
    }
}

@Repository
@EnableScan
interface QueriesDynamoDbRepository : CrudRepository<SongEntity, String> {
    fun findAllBySongName(songName: String): List<SongEntity>
    fun findAllByAlbumName(songName: String): List<SongEntity>
}

@DynamoDBTable(tableName = "SongEntity")
data class SongEntity(

    @DynamoDBHashKey(attributeName = "id")
    var id: String? = null,

    @DynamoDBIndexHashKey(attributeName = "songName", globalSecondaryIndexName = "index_song_name")
    var songName: String? = null,

    @DynamoDBIndexHashKey(attributeName = "albumName", globalSecondaryIndexName = "index_album_name")
    var albumName: String? = null,

    @DynamoDBAttribute(attributeName = "artist")
    var artist: String? = null,

    @DynamoDBAttribute(attributeName = "year")
    var year: String? = null
)
