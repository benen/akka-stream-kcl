package com.benencahill.stream

import java.net.ServerSocket

/**
  * Created by benen on 15/05/17.
  */
package object kinesis {
  def freePort: Int = {
    val socket = new ServerSocket(0)
    socket.getLocalPort
  }
}
