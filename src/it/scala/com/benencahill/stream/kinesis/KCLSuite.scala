package com.benencahill.stream.kinesis

import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration}
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Future
import scala.util.{Random, Try}

trait KCLSuite extends DynamoDbSuite with KinesisSuite with DockerTestKit with BeforeAndAfterAll { self: Suite =>

  val streamName: String = s"${this.suiteName}-kcl-stream"
  val consumerName: String = s"${this.suiteName}-kcl-consumer"
  val workerId = s"${this.suiteName}-worker"
  val numShards: Int = 2

  import KCLSuite._

  val kclConf = new KinesisClientLibConfiguration(consumerName, streamName, credentialsProvider, workerId)
    .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
    .withDynamoDBEndpoint(s"http://localhost:$dynamoDbPort")
    .withKinesisEndpoint(s"http://localhost:$kinesisPort")
    .withMetricsLevel("NONE")

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try { kinesisClient.createStream(streamName, numShards) }
    while (kinesisClient.describeStream(streamName).getStreamDescription.getStreamStatus() != ActiveStatus ) {
      TimeUnit.SECONDS.sleep(1)
    }
  }

  override def afterAll(): Unit = {
    Try { kinesisClient.deleteStream(streamName) }
    super.afterAll()
  }

  def enqueue(numMsgs: Int) = { Future {
    for (_ <- 0 until numMsgs)
      kinesisClient.putRecord(streamName, ByteBuffer.wrap(createMsg.getBytes()), UUID.randomUUID().toString)
  }}
}

object KCLSuite {
  val ActiveStatus = "ACTIVE"

  private val words = Seq("hello", "world", "benen", "was", "here", "how", "are", "you", "spaghetti", "horse", "dog", "cow", "I")

  private def createMsg = Random.shuffle(words).take(Random.nextInt(words.size - 1) + 1).mkString(" ")
}
