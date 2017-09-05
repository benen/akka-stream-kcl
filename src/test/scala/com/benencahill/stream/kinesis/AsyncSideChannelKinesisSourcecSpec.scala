package com.benencahill.stream.kinesis

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.testkit.TestKit
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.model.Record
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by benen on 23/05/17.
  */
class AsyncSideChannelKinesisSourcecSpec extends TestKit(ActorSystem("AsyncSideChannelKinesisSourceSpec"))
  with FlatSpecLike with Eventually with Matchers with MockitoSugar {

  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  behavior of classOf[AsyncSideChannelKinesisSourcecSpec].getSimpleName

  it should "start the kinesis worker upon materialization" in {
    // given
    val fakeProvider = FakeWorkerProvider(Seq(mock[Record]))
    val tested = Source.fromGraph(new AsyncSideChannelKinesisSource(fakeProvider, 1))

    // when
    Await.ready(tested.take(1).runWith(Sink.ignore), 1 seconds)

    // then
    verify(fakeProvider.worker).run
  }

  it should "shutdown the kinesis worker upon termination of the stream" in {
    // given
    val fakeProvider = FakeWorkerProvider(Seq(mock[Record]))
    val tested = Source.fromGraph(new AsyncSideChannelKinesisSource(fakeProvider, 1))

    // when
    Await.ready(tested.take(1).runWith(Sink.ignore), 1 seconds)

    // then
    verify(fakeProvider.worker).requestShutdown()
  }

  it should "shutdown the kinesis worker when the buffer overflows" in {
    // given
    val fakeProvider = FakeWorkerProvider(Seq.fill(3)(mock[Record]))
    val tested = Source.fromGraph(new AsyncSideChannelKinesisSource(fakeProvider, 1))

    // when
    Await.ready(tested.take(3).runWith(Sink.ignore), 3 seconds)

    // then
    verify(fakeProvider.worker, times(2)).requestShutdown()
  }

  it should "resume the stream when the buffer is drained" in {
    // given
    val fakeProvider = FakeWorkerProvider(Seq.fill(3)(mock[Record]))
    val tested = Source.fromGraph(new AsyncSideChannelKinesisSource(fakeProvider, 1))

    // when
    val kSwitch = tested.viaMat(KillSwitches.single)(Keep.right).toMat(Sink.ignore)(Keep.left).run()

    // then
    eventually {
      verify(fakeProvider.worker, times(2)).run
    }
    kSwitch.shutdown()
  }

  case class FakeWorkerProvider(records: Seq[Record], worker: Worker = mock[Worker]) extends KinesisWorkerProvider {
    var delivered = false

    override def instance(process: (Seq[Record]) => Unit): Worker = {
      if (!delivered) {
        process(records)
        delivered = true
      }
      worker
    }
  }

}
