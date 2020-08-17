package io.paytouch.utils

import java.net.ServerSocket

import scala.util.control.NonFatal

trait AvailablePortDiscovery {

  def randomAvailablePort = {
    import util.Random.nextInt
    LazyList
      .continually(nextInt(65000 - 20000))
      .map(_ + 20000)
      .find(portAvailable)
      .getOrElse(new ServerSocket(0).getLocalPort)
  }

  private def portAvailable(p: Int): Boolean = {
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(p)
      true
    }
    catch {
      case NonFatal(_) => false
    }
    finally if (socket != null)
      try socket.close()
      catch {
        case NonFatal(_) =>
      }
  }
}
