package com.benencahill.stream

import java.net.ServerSocket

package object kinesis {
  def freePort: Int = {
    val socket = new ServerSocket(0)
    socket.getLocalPort
  }
}
