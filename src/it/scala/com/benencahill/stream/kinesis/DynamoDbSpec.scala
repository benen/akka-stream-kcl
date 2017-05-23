package com.benencahill.stream.kinesis

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker, LogLineReceiver}
import org.scalatest.Suite

import scala.concurrent.duration._

/**
  * Created by benen on 15/05/17.
  */
trait DynamoDbSpec extends DockerKit { self: Suite =>

  val dynamoDbPort = freePort

  val dynamoDbContainer = DockerContainer(DynamoDbSpec.DynamoDbContainerId)
    .withLogLineReceiver(LogLineReceiver(true, (s : String) => println ("*** " + s) ))
    .withPorts(DynamoDbSpec.DynamoDbPort -> Some(dynamoDbPort))
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(DynamoDbSpec.DynamoDbPort, code = 400)
      .within(100.millis)
      .looped(100, 1250.millis))

  abstract override def dockerContainers: List[DockerContainer] =
    dynamoDbContainer :: super.dockerContainers
}

object DynamoDbSpec {
  val DynamoDbContainerId = "instructure/dynamodb"
  val DynamoDbPort = 8000
}
