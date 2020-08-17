package io.paytouch.ordering.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{ Sink, Source }

class Client(host: String, port: Int)(implicit system: ActorSystem) extends FutureHelpers {
  lazy val flow = Http().outgoingConnection(host, port)

  implicit def toRichHttpRequest(r: HttpRequest): RichHttpRequest = new RichHttpRequest(r)

  class RichHttpRequest(r: HttpRequest) {
    def ==>[T](f: HttpResponse => T): T = {
      val response = Source.single(r).via(flow).runWith(Sink.head)
      f(response.await)
    }
  }
}
