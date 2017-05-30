package com.benencahill.stream.kinesis

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker, LogLineReceiver}
import org.scalatest.Suite

import scala.concurrent.duration._

trait DynamoDbSuite extends DockerKit { self: Suite =>

  val dynamoDbPort = freePort

  val dynamoDbContainer = DockerContainer(DynamoDbSuite.DynamoDbContainerId)
    .withLogLineReceiver(LogLineReceiver(true, (s : String) => println ("*** " + s) ))
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
