package com.benencahill.stream.kinesis

import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker, LogLineReceiver}

import scala.concurrent.duration._

/**
  * Created by benen on 15/05/17.
  */
trait KinesisSpec extends DockerKit {

  System.setProperty(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, true.toString)

  val credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("access_key_id", "secret_key_id"))

  val kinesisPort = freePort

  val kinesisContainer = DockerContainer(KinesisSpec.KinesisContainerId)
    .withLogLineReceiver(LogLineReceiver(true, (s : String) => println ("*** " + s) ))
    .withPorts(KinesisSpec.KinesisPort -> Some(kinesisPort))
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(KinesisSpec.KinesisPort, code = 403)
      .within(1000.millis)
      .looped(1000, 1250.millis))

  val kinesisClient = AmazonKinesisClientBuilder.standard()
    .withCredentials(credentialsProvider)
    .withEndpointConfiguration(new EndpointConfiguration(s"http://localhost:$kinesisPort", "es-west-1"))
    .build()

  abstract override def dockerContainers: List[DockerContainer] =
    kinesisContainer :: super.dockerContainers
}

object KinesisSpec {
  val KinesisContainerId = "instructure/kinesalite"
  val KinesisPort = 4567
}
