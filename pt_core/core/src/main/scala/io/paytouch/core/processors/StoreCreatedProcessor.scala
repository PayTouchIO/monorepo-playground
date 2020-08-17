package io.paytouch.core.processors

import java.util.UUID
import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.messages.entities.{ SQSMessage, StoreCreated }
import io.paytouch.core.entities.enums.{ MerchantSetupSteps, StoreType }
import io.paytouch.core.services._

class StoreCreatedProcessor(
    val locationSettingsService: LocationSettingsService,
    val merchantService: MerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: StoreCreated => processStoreCreated(msg)
  }

  private def processStoreCreated(msg: StoreCreated): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val locationId = msg.payload.data.locationId

    msg.payload.data.`type` match {
      case StoreType.DeliveryProvider =>
        locationSettingsService.setDeliveryProvidersEnabled(merchantId, locationId).void
        merchantService.completeSetupStepByMerchantId(merchantId, MerchantSetupSteps.ConnectDeliveryProvider).void
      case StoreType.Storefront =>
        locationSettingsService.setOnlineStorefrontEnabled(merchantId, locationId).void
        merchantService.completeSetupStepByMerchantId(merchantId, MerchantSetupSteps.SetupOnlineStore).void
    }
  }
}
