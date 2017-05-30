package com.benencahill.stream.kinesis

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import com.whisk.docker.impl.spotify.DockerKitSpotify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class AsyncSideChannelKinesisSourceSpec
  extends FlatSpec
    with DockerKitSpotify
    with KCLSuite
    with ScalaFutures
    with Matchers {

  implicit val pc = PatienceConfig(Span(120, Seconds), Span(1, Second))

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  behavior of "KinesisSource"

  it should "Read a single element from the stream" in {
    // given
    enqueue(1)

    // when
    val results = AsyncSideChannelKinesisSource(kclConf).take(1).toMat(Sink.seq)(Keep.right).run()

    // then
    whenReady(results) { collected =>
      collected should have size 1
    }
  }

  it should "Read more elements than the buffer can handle" in {
    // given
    enqueue(20000)

    // when
    val results = AsyncSideChannelKinesisSource(kclConf, 10000).take(20000).toMat(Sink.seq)(Keep.right).run()

    // then
    whenReady(results) { collected =>
      collected should have size 20000
    }
  }

}
