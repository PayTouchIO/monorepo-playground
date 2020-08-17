package io.paytouch.ordering.clients

import scala.concurrent.duration._
import scala.concurrent._

import akka.http.scaladsl.model._
import akka.pattern._
import akka.testkit.TestProbe
import akka.util.Timeout

import org.specs2.specification.Scope

import io.paytouch.logging.LoggedRequest
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.utils._
import io.paytouch.utils.HttpClient

trait ClientSpec extends PaytouchSpec {
  def parse(in: String): JValue = JsonSupport.fromJsonStringToJson(in)

  implicit val system = MockedRestApi.testAsyncSystem
  implicit val mdcActor = MockedRestApi.mdcActor
  implicit val materializer = MockedRestApi.materializer

  override implicit def ec: ExecutionContext = system.dispatcher

  val host = "example.com"
  val user = "user"
  val pass = "pass"
  val uri = Uri(s"http://$host")

  trait ClientSpecContext extends HttpClient with SendAndReceiveMocker with FixturesSupport with Scope

  trait SendAndReceiveMocker { self: HttpClient =>

    implicit val timeout: Timeout = 10 seconds

    protected val transport = TestProbe()

    override protected def sendRequest[T](request: HttpRequest, f: HttpResponse => Future[T]): Future[T] =
      (transport.ref ? request).map(_.asInstanceOf[HttpResponse]).flatMap(f)

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
          HttpResponse(status = status, entity = HttpEntity(MediaTypes.`application/json`, loadResource(resourcePath))),
        )

      def respondWithOk: Future[T] = {
        val response =
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(MediaTypes.`application/json`, """{"code": "OK"}"""),
          )
        transport.reply(response)
        future
      }

      def respondWithRejection(resourcePath: String): Future[T] =
        respondWith(resourcePath, StatusCodes.BadRequest)

      def respondWithNoContent = {
        val response =
          HttpResponse(status = StatusCodes.NoContent, entity = HttpEntity(MediaTypes.`application/json`, ""))
        transport.reply(response)
        future
      }
    }

    implicit def toRichHttpEntity(entity: HttpEntity): RichHttpEntity = new RichHttpEntity(entity)

    class RichHttpEntity(entity: HttpEntity) {
      val body = entity.withoutSizeLimit.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).await
    }
  }

}
