package com.benencahill.stream.kinesis

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by benen on 23/05/17.
  */
class AsyncSideChannelKInesisSourcecSpec extends TestKit(ActorSystem("AsyncSideChannelKinesisSourceSpec"))
  with FlatSpecLike with ScalaFutures with Matchers {

  implicit val materializer = ActorMaterializer()
  implicit val context = system.dispatcher

  behavior of classOf[AsyncSideChannelKInesisSourcecSpec].getSimpleName

  it should "start the kinesis worker upon materialization" in {
    val tested = new AsyncSideChannelKinesisSource()
  }

  it should "shutdown the kinesis worker upon termination of the stream" in {
    pending
  }

  it should "shutdown the kinesis worker when the buffer overflows in" in {

  }

  it should "resume the stream when the buffer is drained" in {

  }

  // TODO: I could use a timed stage to track the rate being delivered downstream relative to the rate being dequeued
  // TODO: and pause the worker temporarily if it seems we're not pushing elements fast enough


}
