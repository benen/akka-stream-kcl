package com.benencahill.stream.kinesis

import akka.stream.scaladsl.SourceQueue
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory, IShutdownNotificationAware}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record

import scala.concurrent.Await
import scala.collection.JavaConverters._

class RecordProcessorFactory(val process: Seq[Record] => Unit) extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = new IRecordProcessor with IShutdownNotificationAware {

    override def initialize(initializationInput: InitializationInput): Unit = {}

    override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
      process(processRecordsInput.getRecords.asScala)
      processRecordsInput.getCheckpointer.checkpoint()
    }

    override def shutdown(shutdownInput: ShutdownInput): Unit = {}

    override def shutdownRequested(checkpointer: IRecordProcessorCheckpointer): Unit = {
      checkpointer.checkpoint()
    }
  }
}
