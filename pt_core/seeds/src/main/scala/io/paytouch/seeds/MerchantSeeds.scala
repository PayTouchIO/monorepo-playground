package io.paytouch.seeds

import scala.concurrent._

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.MerchantFeatures
import io.paytouch.seeds.IdsProvider._

object MerchantSeeds extends Seeds {
  lazy val merchantDao = daos.merchantDao

  def load(email: String): Future[MerchantRecord] = {
    val merchantId = merchantIdPerEmail(email)
    val businessType = businessTypePerEmail(email)

    val merchant =
      MerchantUpdate(
        id = Some(merchantId),
        active = None,
        businessType = Some(businessType),
        businessName = Some(s"$email's Business"),
        restaurantType = Some(RestaurantType.Default),
        paymentProcessor = Some(PaymentProcessor.Worldpay),
        paymentProcessorConfig = ServiceConfigurations.worldpay.some,
        currency = Some(USD),
        mode = Some(MerchantMode.Production),
        switchMerchantId = None,
        defaultZoneId = Some(genZoneId.instance),
        setupSteps = None,
        setupCompleted = None,
        features = None,
        legalDetails = None,
        setupType = SetupType.Paytouch.some,
      )

    merchantDao.upsert(merchant).extractRecord(email)
  }
}
