package com.benencahill.stream.kinesis

import akka.stream.scaladsl.Source
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.model.Record
import akka.stream.{ActorMaterializerHelper, SourceShape}

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.collection.JavaConverters._

/**
  * Created by benen on 16/05/17.
  */
class BufferingKinesisSource(config: KinesisClientLibConfiguration) extends GraphStage[SourceShape[Record]] {

  val out: Outlet[Record] = Outlet("KinesisSource")

  override def shape: SourceShape[Record] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    val buffer = new java.util.concurrent.LinkedTransferQueue[Record]()

    def process: Seq[Record] => Unit = { records =>
      println(s"Adding ${records.size} records to buffer")
      records.foreach(buffer.put)
      attemptPush
    }

    val worker = KinesisWorkerProvider(config, process)
    var demand = true
    var seq = 0

    override def preStart(): Unit = {
      super.preStart()
      implicit val context = this.materializer.executionContext
      Future { worker.run() }
    }

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        println("I've been pulled")
        if (!buffer.isEmpty) {
          attemptPush
        }
        else {
          println(s"Waiting for elements")
          demand = true
        }
      }

      override def onDownstreamFinish(): Unit = {
        worker.requestShutdown()
        super.onDownstreamFinish()
      }
    })

    def attemptPush = {
      this.synchronized {
        if (!buffer.isEmpty && demand) {
          seq += 1
          println(s"Pushing an element downstream, record number: $seq")
          push(out, buffer.take)
          println(s"Element pushed")
          demand = false
        }
      }
    }
  }
}

object BufferingKinesisSource {
  def apply(config: KinesisClientLibConfiguration): Source[Record, _] = Source.fromGraph(new BufferingKinesisSource(config))
}
