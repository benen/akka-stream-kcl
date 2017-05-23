package com.benencahill.stream.kinesis

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.model.Record

import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.duration._

/**
  * Created by benen on 22/05/17.
  */
class AsyncSideChannelKinesisSource(kinesisWorkerProvider: KinesisWorkerProvider) extends GraphStage[SourceShape[Record]] {

  val out = Outlet[Record]("KinesisSource.out")
  val shape = SourceShape.of(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {

      var worker: Worker = _
      val queue = scala.collection.mutable.Queue.empty[Record]
      var demand = false

      override def preStart(): Unit = {
        log.debug(s"Setting up the callback")
        val callback = getAsyncCallback[Seq[Record]] { records =>
          if (demand) {
            log.debug(s"Sending downstream")
            queue.enqueue(records.tail: _*)
            demand = false
            push(out, records.head)
          }
          else {
            log.debug(s"Buffering for now")
            queue.enqueue(records: _*)
          }
        }
        val process: Seq[Record] => Unit = callback.invoke
        log.debug(s"Starting the worker")
        worker = kinesisWorkerProvider.instance(process)
        Future {
          blocking {
            worker.run()
          }
        }(materializer.executionContext)
      }

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          log.debug(s"I've been pulled")
          if (queue.nonEmpty) {
            log.debug("pushing downstream")
            push(out, queue.dequeue())
          }
          else {
            log.debug(s"Flagging demand")
            demand = true
          }
        }

        override def onDownstreamFinish(): Unit = {
          log.debug(s"Shutting down the Kinesis worker")
          val shutdownAttempt = Future {
            blocking {
              worker.requestShutdown().get
            }
          }(materializer.executionContext).andThen { case res =>
            log.debug(s"Worker shutdown with result $res")
            worker.shutdown()
          }(materializer.executionContext)
          log.debug(s"Doing a horrid await. Thanks Amazon")
          Await.ready(shutdownAttempt, 30 seconds)
          super.onDownstreamFinish()
        }
      })
    }
}

object AsyncSideChannelKinesisSource {
  def apply(kclConfig: KinesisClientLibConfiguration): Source[Record, NotUsed] =
    Source.fromGraph(new AsyncSideChannelKinesisSource(new KinesisWorkerProvider(kclConfig)))
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
}


