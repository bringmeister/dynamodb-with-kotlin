package de.bringmeister.infrastructure.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.Projection
import com.amazonaws.services.dynamodbv2.model.ProjectionType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class DynamoDbTableUtil {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var amazonDynamoDB: AmazonDynamoDB

    var pt = ProvisionedThroughput(1L, 1L)

    fun createTableForEntity(entity: KClass<*>) {

        val tableRequest = DynamoDBMapper(amazonDynamoDB)
            .generateCreateTableRequest(entity.java)
            .withProvisionedThroughput(pt)

        if (tableRequest.globalSecondaryIndexes != null) {
            tableRequest.globalSecondaryIndexes.forEach { gsi ->
                gsi.projection = Projection().withProjectionType(ProjectionType.ALL)
                gsi.provisionedThroughput = pt
            }
        }

        // We always try to create the table. If it already exists, AWS will return an error. We just
        // write a log message in this case and ignore the exception.  This is the same behaviour as
        // the AWS SDK uses internally, for example in:
        //
        // com.amazonaws.services.kinesis.leases.impl.LeaseManager#createLeaseTableIfNotExists(...)

        try {
            DynamoDB(amazonDynamoDB)
                .createTable(tableRequest)
                .waitForActive()
            log.info("Table created! [entity={}]", entity)
        } catch (e: ResourceInUseException) {
            log.info("Table already exists - skip creation! [entity={}]", entity)
        }
    }
}
