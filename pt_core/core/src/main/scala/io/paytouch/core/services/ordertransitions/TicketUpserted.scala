package io.paytouch.core.services.ordertransitions

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import io.paytouch.implicits._
import io.paytouch.core.RichZoneDateTime
import io.paytouch.core.data.model
import io.paytouch.core.services
import io.paytouch.core.utils.UtcTime
import Errors._

trait TicketUpserted extends LazyLogging with Computations.ComputeStatus {
  import TicketUpserted._

  def apply(
      order: model.OrderRecord,
      tickets: Seq[model.TicketRecord],
      autoCompleteEnabled: Boolean,
      locationZoneId: ZoneId,
    ): ErrorsAndResult[Option[model.OrderUpdate]] = {
    val resultStatus =
      computeStatus(order, tickets, autoCompleteEnabled, locationZoneId)

    val update = resultStatus.data.deriveUpdateFromPreviousState(order)

    resultStatus.copy(data = update)
  }
}

object TicketUpserted extends TicketUpserted
