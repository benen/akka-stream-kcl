package com.benencahill.stream.kinesis

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker, LogLineReceiver}
import org.scalatest.Suite
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

trait DynamoDbSuite extends DockerKit {
  self: Suite =>

  private val logger = LoggerFactory.getLogger(this.getClass())

  val dynamoDbPort = freePort

  val dynamoDbContainer = DockerContainer(DynamoDbSuite.DynamoDbContainerId)
    .withLogLineReceiver(LogLineReceiver(true, (line: String) => logger.debug(line)))
    .withPorts(DynamoDbSuite.DynamoDbPort -> Some(dynamoDbPort))
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(DynamoDbSuite.DynamoDbPort, code = 400)
      .within(1000.millis)
      .looped(1000, 1250.millis))

  abstract override def dockerContainers: List[DockerContainer] =
    dynamoDbContainer :: super.dockerContainers
}

object DynamoDbSuite {
  val DynamoDbContainerId = "instructure/dynamodb"
  val DynamoDbPort = 8000
}
