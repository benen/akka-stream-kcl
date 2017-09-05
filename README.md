Akka Streams Kinesis Client 
================

An **experimental** wrapper around the KCL that provides an Akka streams source that consumes from Kinesis. 
Creates an instance of the KCL worker that dispatches all records to a materialiazed stream using
the SourceQueue API. 

This implementation is intended to avoid blocking within the KCL worker threads, instead relying on the AsyncCallback
API to populate an internal buffer. When the internal buffer overflows, we shutdown the KCL worker. When the buffer 
is drained, we start up a new instance of the KCL. 

For an example of usage, see [the integration tests](./src/it/scala/com/benencahill/stream/kinesis/AsyncSideChannelKinesisSourceSpec.scala)
