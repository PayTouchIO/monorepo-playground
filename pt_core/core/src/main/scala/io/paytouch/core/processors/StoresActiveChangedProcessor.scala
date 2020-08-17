package io.paytouch.core.processors

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.messages.entities.{ SQSMessage, StoresActiveChanged }
import io.paytouch.core.services._

class StoresActiveChangedProcessor(
    val locationSettingsService: LocationSettingsService,
    val merchantService: MerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: StoresActiveChanged => processStoresActiveChanged(msg)
  }

  private def processStoresActiveChanged(msg: StoresActiveChanged): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val locationItems = msg.payload.data

    // Temporary until ordering is changed to set StoreChanged messages and
    // everything is deployed. After that this processor can be deleted.
    locationItems.foreach { item =>
      if (item.active)
        locationSettingsService.setOnlineStorefrontEnabled(merchantId, item.itemId).void
    }
    merchantService.completeSetupStepByMerchantId(merchantId, MerchantSetupSteps.SetupOnlineStore).void
  }
}
