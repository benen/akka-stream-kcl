Akka Streams Kinesis Client 
================

A simple wrapper around the KCL that provides an Akka streams source that consumes from Kinesis. 
Creates an instance of the KCL worker that dispatches all records to a materialiazed stream using
the SourceQueue API.

## To Do

* [x] Create a ScalaTest suite that creates a Kinesis Stream using docker
  * [x] Create a Suite mixin that starts Kinesis and DynamoDB and cleans up after
  * [x] Await docker container startup
* [x] Implement a KCL worker factory
* [x] Implement a custom GraphStage that implements asynchronous side channels:
    http://doc.akka.io/docs/akka/2.5/scala/stream/stream-customize.html#Using_asynchronous_side-channels
* [ ] Implement Unit tests for AsyncSideChannelKinesisSource
  * [ ] Test retry given failure/timeout case

## Options

* Implement an ActorPublisher with receives messages from another actor that manages the
  KCL worker
* Implement a clone of the SourceQueue src which simply spawns a KCL worker instead of a source
  queue
* Stop the worker when the buffer goes beyond a certain limit. 
* Stop the worker if the rate of elements getting dequeued from Kinesis is significantly
  above the rate being pushed downstream
  

## Known Obstacles

1. Implementing a GraphStage rules out the use of a SourceQueue, would need to
   maintain an internal buffer of messages and shutdown the worker if it grows to big.    
2. Kinesis worker threads operate entirely independently and their behavior is 
   unpredictable. It is not possible to adequately synchronize them if using a shared
   queue without resorting to asynchronous callbacks. 