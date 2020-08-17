package io.paytouch.core.processors

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.messages.entities.{ RapidoChanged, SQSMessage }
import io.paytouch.core.services._

class RapidoChangedProcessor(
    val locationSettingsService: LocationSettingsService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: RapidoChanged => processRapidoChanged(msg)
  }

  private def processRapidoChanged(msg: RapidoChanged): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val locationItems = msg.payload.data
    locationSettingsService.updateRapidoActive(merchantId, locationItems).void
  }
}
