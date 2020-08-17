package io.paytouch.core.services.ordertransitions

import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services
import io.paytouch.core.utils._

@scala.annotation.nowarn("msg=Auto-application")
class TicketUpsertedSpec extends PaytouchSpec {
  import TicketUpserted._
  case class ErrorSpy(val message: String) extends Errors.Error
  val computeStatusErrorSpy = ErrorSpy("computeStatus")

  "TicketUpserted" should {
    "call all computations in order" in {
      val subject = new TicketUpserted {
        override def computeStatus(
            order: model.OrderRecord,
            tickets: Seq[model.TicketRecord],
            isAutocompleteEnabled: Boolean,
            locationZoneId: ZoneId,
          ): Errors.ErrorsAndResult[model.OrderRecord] = {
          val pickValueToTriggerAnUpdate = model.enums.OrderStatus.values.find(_.some != order.status)
          Errors.ErrorsAndResult(Seq(computeStatusErrorSpy), order.copy(status = pickValueToTriggerAnUpdate))
        }
      }

      prop {
        (
            order: model.OrderRecord,
            tickets: Seq[model.TicketRecord],
            autoCompleteEnabled: Boolean,
            locationZoneId: ZoneId,
        ) =>
          val errorAndResult =
            subject(
              order,
              tickets,
              autoCompleteEnabled,
              locationZoneId,
            )

          errorAndResult.errors ==== Seq(
            computeStatusErrorSpy,
          )
          val result = errorAndResult.data
          result must beSome
          result.get.status must beSome
      }
    }

    "return None if no change happens" in {
      val subject = new TicketUpserted {
        override def computeStatus(
            order: model.OrderRecord,
            tickets: Seq[model.TicketRecord],
            isAutocompleteEnabled: Boolean,
            locationZoneId: ZoneId,
          ): Errors.ErrorsAndResult[model.OrderRecord] =
          Errors.ErrorsAndResult(Seq(computeStatusErrorSpy), order)
      }

      prop {
        (
            order: model.OrderRecord,
            tickets: Seq[model.TicketRecord],
            autoCompleteEnabled: Boolean,
            locationZoneId: ZoneId,
        ) =>
          val errorAndResult =
            subject(
              order,
              tickets,
              autoCompleteEnabled,
              locationZoneId,
            )

          errorAndResult.errors ==== Seq(
            computeStatusErrorSpy,
          )
          val result = errorAndResult.data
          result must beNone
      }
    }
  }
}
