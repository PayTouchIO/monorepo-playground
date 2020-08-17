package io.paytouch.ordering.data.daos

import scala.concurrent._

import com.softwaremill.macwire._

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._

final class Daos(implicit val ec: ExecutionContext, val db: Database) {
  lazy val cartDao = wire[CartDao]
  lazy val cartItemDao = wire[CartItemDao]
  lazy val cartItemModifierOptionDao = wire[CartItemModifierOptionDao]
  lazy val cartItemTaxRateDao = wire[CartItemTaxRateDao]
  lazy val cartItemVariantOptionDao = wire[CartItemVariantOptionDao]
  lazy val cartTaxRateDao = wire[CartTaxRateDao]
  lazy val merchantDao = wire[MerchantDao]
  lazy val paymentIntentDao = wire[PaymentIntentDao]
  lazy val paymentProcessorCallbackDao = wire[PaymentProcessorCallbackDao]
  lazy val storeDao = wire[StoreDao]
  lazy val worldpayPaymentDao = wire[WorldpayPaymentDao]
}
