package io.paytouch.core.services.ordertransitions

import java.time.{ ZoneId, ZonedDateTime }

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary

import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.services.OrderService
import io.paytouch.core.utils.PaytouchSpec

@scala.annotation.nowarn("msg=Auto-application")
class ComputationsComputeStatusSpec extends PaytouchSpec {
  // downgrade to non-implicit
  override val arbitraryOrderStatus: Arbitrary[model.enums.OrderStatus] =
    Arbitrary(genOrderStatus)
  override val arbitraryPaymentStatus: Arbitrary[model.enums.PaymentStatus] =
    Arbitrary(genPaymentStatus)
  override val arbitraryTicketStatus: Arbitrary[entities.enums.TicketStatus] =
    Arbitrary(genTicketStatus)

  implicit val arbStatusNeverCompleted: Arbitrary[model.enums.OrderStatus] =
    Arbitrary(Gen.oneOf(model.enums.OrderStatus.values.filterNot(_ == model.enums.OrderStatus.Completed)))

  "Computations.ComputeStatus" should {
    val subject = new Computations.ComputeStatus with LazyLogging {}

    "if at least one ticket is new or in progress" in {
      "mark as InProgress" in {
        implicit val arbitraryTicketStatus: Arbitrary[entities.enums.TicketStatus] =
          Arbitrary(Gen.oneOf(entities.enums.TicketStatus.isNewOrInProgress))
        implicit val arbAtLeastOneTicket: Arbitrary[Seq[model.TicketRecord]] = Arbitrary(
          Gen.nonEmptyContainerOf[Seq, model.TicketRecord](Arbitrary.arbitrary[model.TicketRecord]),
        )
        prop {
          (
              order: model.OrderRecord,
              tickets: Seq[model.TicketRecord],
              isAutocompleteEnabled: Boolean,
              locationZoneId: ZoneId,
          ) =>
            val errorAndResult = subject.computeStatus(order, tickets, isAutocompleteEnabled, locationZoneId)
            errorAndResult.errors must beEmpty
            errorAndResult.data.status ==== model.enums.OrderStatus.InProgress.some
        }
      }
    }

    "if isAutocompleteEnabled = false" in {
      val isAutocompleteEnabled = false
      "never complete an incomplete order" in {
        prop {
          (
              order: model.OrderRecord,
              tickets: Seq[model.TicketRecord],
              locationZoneId: ZoneId,
          ) =>
            val errorAndResult = subject.computeStatus(order, tickets, isAutocompleteEnabled, locationZoneId)
            errorAndResult.errors must beEmpty
            errorAndResult.data.status !=== model.enums.OrderStatus.Completed.some
        }
      }

    }

    "if isAutocompleteEnabled = true" in {
      val isAutocompleteEnabled = true
      "if no tickets or all completed tickets" in {
        implicit val arbitraryTicketStatus: Arbitrary[entities.enums.TicketStatus] =
          Arbitrary(Gen.const(entities.enums.TicketStatus.Completed))
        "if order payment status is positive" in {
          implicit val arbPaymentStatusPositive: Arbitrary[Option[model.enums.PaymentStatus]] =
            Arbitrary(Gen.some(Gen.oneOf(model.enums.PaymentStatus.values.filter(_.isPositive))))
          "mark as Completed" in {
            prop {
              (
                  order: model.OrderRecord,
                  tickets: Seq[model.TicketRecord],
                  locationZoneId: ZoneId,
              ) =>
                val errorAndResult = subject.computeStatus(order, tickets, isAutocompleteEnabled, locationZoneId)
                errorAndResult.errors must beEmpty
                val result = errorAndResult.data
                result.status ==== model.enums.OrderStatus.Completed.some
                result.completedAt !=== order.completedAt
                result.completedAtTz !=== order.completedAtTz
            }
          }
        }
        "if order payment status is not positive" in {
          implicit val arbPaymentStatusPositive: Arbitrary[Option[model.enums.PaymentStatus]] =
            Arbitrary(Gen.option(Gen.oneOf(model.enums.PaymentStatus.values.filterNot(_.isPositive))))
          "not change status" in {
            prop {
              (
                  order: model.OrderRecord,
                  tickets: Seq[model.TicketRecord],
                  locationZoneId: ZoneId,
              ) =>
                val errorAndResult = subject.computeStatus(order, tickets, isAutocompleteEnabled, locationZoneId)
                errorAndResult.errors must beEmpty
                errorAndResult.data.status ==== order.status
            }
          }
        }
      }
    }
  }
}
