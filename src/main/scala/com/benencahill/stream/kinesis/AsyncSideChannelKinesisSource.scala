package com.benencahill.stream.kinesis

import akka.NotUsed
import akka.event.Logging
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.model.Record
import com.benencahill.concurrent.Implicits

import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.duration._

class AsyncSideChannelKinesisSource(kinesisWorkerProvider: KinesisWorkerProvider, limit: Int) extends GraphStage[SourceShape[Record]] {

  val out = Outlet[Record]("KinesisSource.out")
  val shape = SourceShape.of(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {

      var worker: Worker = _
      val queue = scala.collection.mutable.Queue.empty[Record]
      var hasDemand = false
      var isPaused = false
      var shutdownInProgress: Future[Any] = _

      override def preStart(): Unit = {
        log.debug(s"Initializing the source and starting the worker")
        startWorker()
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
            hasDemand = true
            if (isPaused) {
              log.debug(s"Buffer has been drained. Starting KCL worker")
              startWorker()
              isPaused = false
            }
          }
        }

        override def onDownstreamFinish(): Unit = {
          log.info(s"Shutting down due to downstream signal")
          shutdownWorker()
          super.onDownstreamFinish()
        }
      })

      private def getCallBack: AsyncCallback[Seq[Record]] = getAsyncCallback[Seq[Record]] { records =>
        if (hasDemand) {
          log.debug(s"Sending downstream")
          queue.enqueue(records.tail: _*)
          hasDemand = false
          push(out, records.head)
        }
        else {
          log.debug(s"Buffering for now")
          queue.enqueue(records: _*)
        }

        if (queue.size > limit) {
          log.debug(s"Buffer exceeds limit, shutting down KCL")
          shutdownWorker()
          isPaused = true
        } else {
          log.debug(s"There are ${queue.size} elements on the buffer")
        }
      }

      private def startWorker(): Unit = {
        implicit val context = materializer.executionContext

        if (shutdownInProgress == null || shutdownInProgress.isCompleted) {
          log.info(s"No prior shutdown attempt present. Invoking the worker")
          worker = kinesisWorkerProvider.instance(getCallBack.invoke)
          Future {
            blocking {
              worker.run()
            }
          }
        }
        else {
          log.info(s"Waiting for shutdown to complete before starting worker")
          shutdownInProgress andThen { case _ =>
            worker = kinesisWorkerProvider.instance(getCallBack.invoke)
            blocking {
              worker.run()
            }
          }
        }
      }

      private def shutdownWorker(): Unit = {
        implicit val context = materializer.executionContext
        log.info(s"Shutting down the Kinesis worker")
        shutdownInProgress = Implicits.javaFutureAsScala(worker.requestShutdown())
        shutdownInProgress.onComplete(_ => worker.shutdown())
        Await.ready(shutdownInProgress, Duration.Inf)
        log.info(s"Worker shutdown complete")
      }
    }
}

object AsyncSideChannelKinesisSource {
  def apply(kclConfig: KinesisClientLibConfiguration, limit: Int = 1000000): Source[Record, NotUsed] =
    Source.fromGraph(new AsyncSideChannelKinesisSource(KinesisWorkerProvider(kclConfig), limit))
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
}


