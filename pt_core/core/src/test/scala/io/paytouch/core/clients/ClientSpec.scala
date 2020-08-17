package io.paytouch.core.clients

import akka.http.scaladsl.model._
import akka.testkit.TestProbe
import akka.pattern._
import akka.util.Timeout
import io.paytouch.core.json.JsonSupport
import io.paytouch.logging.LoggedRequest
import io.paytouch.core.utils._
import io.paytouch.utils.HttpClient
import org.specs2.specification.Scope

import scala.concurrent.duration._
import scala.concurrent._

trait ClientSpec extends PaytouchSpec {
  def parse(in: String): JValue = JsonSupport.fromJsonStringToJson(in)

  implicit val system = MockedRestApi.testAsyncSystem
  implicit val mdcActor = MockedRestApi.mdcActor

  override implicit def ec: ExecutionContext = system.dispatcher

  val host = "example.com"
  val user = "user"
  val pass = "pass"
  val uri = Uri(s"http://$host")

  trait ClientSpecContext extends HttpClient with SendAndReceiveMocker with FixturesSupport with Scope

  trait SendAndReceiveMocker { self: HttpClient =>
    implicit val timeout: Timeout = 10.seconds

    // this code duplicates too much of the production logic
    override protected def sendAndReceive[T: Manifest](request: HttpRequest): Future[Wrapper[T]] =
      (transport.ref ? prepareRequest(request)).map(_.asInstanceOf[HttpResponse]).flatMap { response =>
        processResponse(LoggedRequest(request), request, request.entity.asInstanceOf[HttpEntity.Strict], response)
      }

    protected val transport = TestProbe()

    def when[T](future: Future[T]): RequestMatcher[T] = new RequestMatcher(future)

    class RequestMatcher[T](future: Future[T]) {
      protected def responder = new Responder(future)

      def expectRequest(req: HttpRequest): Responder[T] = {
        transport.expectMsg(req)
        responder
      }

      def expectRequest(fn: HttpRequest => Unit) = {
        transport.expectMsgPF() {
          case req: HttpRequest => fn(req)
        }
        responder
      }
    }

    class Responder[T](future: Future[T]) {
      def respondWith(res: HttpResponse): Future[T] = {
        transport.reply(res); future
      }

      def respondWith(resourcePath: String, status: StatusCode = StatusCodes.OK): Future[T] =
        respondWith(
          HttpResponse(status = status, entity = HttpEntity(MediaTypes.`application/json`, loadJson(resourcePath))),
        )

      def respondWithOk: Future[Unit] = {
        val response =
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(MediaTypes.`application/json`, """{"code": "OK"}"""),
          )
        transport.reply(response)
        Future(())
      }

      def respondWithError(status: StatusCode = StatusCodes.BadRequest): Future[T] =
        respondWith(
          HttpResponse(status = status, entity = HttpEntity(MediaTypes.`application/json`, """{"status":"error"}""")),
        )

      def respondWithRejection(resourcePath: String): Future[T] =
        respondWith(resourcePath, StatusCodes.BadRequest)
    }

    implicit def toRichHttpEntity(entity: HttpEntity): RichHttpEntity = new RichHttpEntity(entity)

    class RichHttpEntity(entity: HttpEntity) {
      val body = entity.withoutSizeLimit.toStrict(5.seconds).map(_.data.decodeString("UTF-8")).await
    }
  }

}
