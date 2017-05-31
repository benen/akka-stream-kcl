package com.benencahill.stream.kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.model.Record

trait KinesisWorkerProvider {
  def instance(process: Seq[Record] => Unit): Worker
}

object KinesisWorkerProvider {

  def apply(kclConf: KinesisClientLibConfiguration): KinesisWorkerProvider =
    (process: (Seq[Record]) => Unit) => worker(kclConf, new RecordProcessorFactory(process))

  private def worker(kclConf: KinesisClientLibConfiguration, factory: IRecordProcessorFactory): Worker =
    new Worker.Builder()
      .recordProcessorFactory(factory)
      .config(kclConf)
      .build()
}
