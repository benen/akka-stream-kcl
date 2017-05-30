package com.benencahill.concurrent

import java.util.concurrent.{Future => JFuture}

import scala.concurrent.{ExecutionContext, Future => SFuture, blocking}


object Implicits {
  implicit def javaFutureAsScala[T](future: JFuture[T])(implicit context: ExecutionContext): SFuture[T] = {
    SFuture { blocking { future.get }}
  }

}
