package throwaway

import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy}
import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory, IShutdownNotificationAware}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.benencahill.stream.kinesis.SourceQueueRecordProcessorFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

/**
  * Created by benen on 12/05/17.
  */
object ThrowAwayExperiment extends App {

  // docker run -d -p 4567:4567 vsouza/kinesis-local --port 4567
  // docker run -d -p 8000:8000 dwmkerr/dynamodb  --port 8000

  val creds = new BasicAWSCredentials("access_key_id", "secret_key_id")
  val credentialsProvider = new AWSStaticCredentialsProvider(creds)

  System.setProperty(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, true.toString)

  val kinesis = AmazonKinesisClientBuilder.standard()
    .withCredentials(credentialsProvider)
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:4567", "es-west-1"))
    .build()

  Try { kinesis.createStream("test-stream", 2) }
  while (kinesis.describeStream("test-stream").getStreamDescription.getStreamStatus() != "ACTIVE" ) {
    TimeUnit.SECONDS.sleep(1)
  }
  val words = Seq("hello", "world", "benen", "was", "here", "how", "are", "you", "spaghetti", "horse", "dog", "cow", "I")
  def msg = Random.shuffle(words).take(Random.nextInt(words.size)).mkString(" ")
  Future {
    while(true) kinesis.putRecord("test-stream", ByteBuffer.wrap(msg.getBytes()), UUID.randomUUID().toString)
  }

  val kclConf = new KinesisClientLibConfiguration("test-application", "test-stream", credentialsProvider, "test")
    .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
    .withDynamoDBEndpoint("http://localhost:8000")
    .withKinesisEndpoint("http://localhost:4567")
    .withMetricsLevel("NONE")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  val src = Source.queue[Record](10000, OverflowStrategy.backpressure)
  val flow = Flow[Record].map { record => new String(record.getData.array()) }
              .viaMat(KillSwitches.single)(Keep.right)
  val sink = Sink.foreach[String](println)
  val (queue, switch, done) = src.viaMat(flow)(Keep.both).toMat(sink)(Keep.both).mapMaterializedValue {
    case ((sourceQueue, killSwitch), futureDone) => (sourceQueue, killSwitch, futureDone)
  }.run()

  val factory = new SourceQueueRecordProcessorFactory(queue)

  val worker = new Worker.Builder()
    .recordProcessorFactory(factory)
    .config(kclConf)
    .build()

  Future { worker.run() }

  TimeUnit.SECONDS.sleep(30)


  val s = worker.requestShutdown()
  switch.shutdown()

  println(s"Shut down requested")

  while (!s.isDone) { TimeUnit.SECONDS.sleep(1) }

  println(s"Shut down complete")

  TimeUnit.SECONDS.sleep(10)

  sys.exit()
}

class PrintlnRecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = new IRecordProcessor with IShutdownNotificationAware {

    override def initialize(initializationInput: InitializationInput): Unit = {
      val shardId = initializationInput.getShardId
      val seqNum = initializationInput.getExtendedSequenceNumber
      println(s"Processing shard $shardId with sequence number $seqNum")
    }

    override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
      val records = processRecordsInput.getRecords.asScala
      records.foreach { record =>
        val msg = new String(record.getData.array())
        val num = record.getSequenceNumber
        println(s"Processing the msg $num which has the value $msg")
      }
      processRecordsInput.getCheckpointer.checkpoint()
    }

    override def shutdown(shutdownInput: ShutdownInput): Unit = {
      println(s"Shutting down for reason ${shutdownInput.getShutdownReason}")
    }

    override def shutdownRequested(checkpointer: IRecordProcessorCheckpointer): Unit = {
      println(s"Shutdown has been requested. Checkpointing")
      checkpointer.checkpoint()
    }
  }
}
