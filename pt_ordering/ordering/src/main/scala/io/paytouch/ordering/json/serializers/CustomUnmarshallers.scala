package io.paytouch.ordering.json.serializers

import java.time._
import java.util.UUID

import scala.collection._
import scala.concurrent._
import scala.util._
import scala.util.control.NoStackTrace

import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.NoContentException
import akka.stream.Materializer

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.stripe.Livemode
import io.paytouch.ordering.utils.UtcTime

trait CustomUnmarshallers {
  implicit object UUIDUnmarshaller extends FromStringUnmarshaller[UUID] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[UUID] =
      Future(UUID.fromString(value))
  }

  implicit object UUIDsUnmarshaller extends FromStringUnmarshaller[Seq[UUID]] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[Seq[UUID]] =
      Future {
        val seq: Seq[UUID] =
          immutable.ArraySeq.unsafeWrapArray(value.split(",").filter(_.trim.nonEmpty).map(UUID.fromString))

        if (seq.nonEmpty)
          seq
        else
          throw NoContentException
      }
  }

  implicit object PositiveBigDecimalUnmarshaller extends FromStringUnmarshaller[BigDecimal] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[BigDecimal] =
      Future {
        Try(BigDecimal(value)) match {
          case Success(bg) if bg < 0 => throw new RuntimeException("BigDecimal cannot be negative") with NoStackTrace
          case Success(bg)           => bg
          case Failure(ex)           => throw ex
        }
      }
  }

  implicit object ZonedDateTimeUnmarshaller extends FromStringUnmarshaller[ZonedDateTime] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[ZonedDateTime] =
      Future(ZonedDateTime.parse(value)) fallbackTo LocalDateTimeUnmarshaller(value).map(UtcTime.ofLocalDateTime)
  }

  implicit object LocalDateTimeUnmarshaller extends FromStringUnmarshaller[LocalDateTime] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[LocalDateTime] =
      Future(LocalDateTime.parse(value)) fallbackTo LocalDateUnmarshaller(value).map(_.atStartOfDay)
  }

  implicit object LocalDateUnmarshaller extends FromStringUnmarshaller[LocalDate] {
    def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[LocalDate] = {
      val dateValue = value.split("T").head
      Future(LocalDate.parse(dateValue))
    }
  }

  implicit object CountryCodeUnmarshaller extends OpaqueStringUnmarshaller(Country.Code)
  implicit object CountryNameUnmarshaller extends OpaqueStringUnmarshaller(Country.Name)

  implicit object GiftCardPassId extends OpaqueStringUnmarshaller(io.paytouch.GiftCardPass.Id)

  implicit object GiftCardPassOnlineCodeRawUnmarshaller
      extends OpaqueStringUnmarshaller(io.paytouch.GiftCardPass.OnlineCode.Raw)

  implicit object OrderId extends OpaqueStringUnmarshaller(io.paytouch.OrderId)

  implicit object StripeLivemodeUnmarshaller extends OpaqueBooleanUnmarshaller(Livemode)
  implicit object StateCodeUnmarshaller extends OpaqueStringUnmarshaller(State.Code)
  implicit object StateNameUnmarshaller extends OpaqueStringUnmarshaller(State.Name)

  class OpaqueStringUnmarshaller[O <: Opaque[String]: Manifest](f: String => O) extends FromStringUnmarshaller[O] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[O] =
      f(value).pure[Future]
  }

  class OpaqueBooleanUnmarshaller[O <: Opaque[Boolean]: Manifest](f: Boolean => O) extends FromStringUnmarshaller[O] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[O] =
      Try(value.toBoolean).getOrElse(false).pipe(f).pure[Future]
  }
}
