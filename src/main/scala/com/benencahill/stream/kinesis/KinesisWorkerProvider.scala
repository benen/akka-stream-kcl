package com.benencahill.stream.kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.model.Record
import com.benencahill.stream.kinesis.KinesisWorkerProvider.worker


class KinesisWorkerProvider(kclConf: KinesisClientLibConfiguration) {
  def instance(process: Seq[Record] => Unit): Worker = worker(kclConf, new RecordProcessorFactory(process))
}

object KinesisWorkerProvider {

  private def worker(kclConf: KinesisClientLibConfiguration, factory: IRecordProcessorFactory): Worker = {
    new Worker.Builder()
      .recordProcessorFactory(factory)
      .config(kclConf)
      .build()
  }
}
