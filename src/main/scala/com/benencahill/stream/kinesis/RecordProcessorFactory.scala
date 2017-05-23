package com.benencahill.stream.kinesis

import akka.stream.scaladsl.SourceQueue
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory, IShutdownNotificationAware}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record

import scala.concurrent.Await
import scala.collection.JavaConverters._

/**
  * Created by benen on 18/05/17.
  */
class RecordProcessorFactory(val process: Seq[Record] => Unit) extends IRecordProcessorFactory {
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
      process(records)
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
