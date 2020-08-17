package io.paytouch.ordering.resources.stripe

import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.data.model.StripeConfig
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor }
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MultipleLocationFixtures }
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

abstract class StripeFSpec extends FSpec with CommonArbitraries {

  abstract class StripeFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val cartDao = daos.cartDao

    override lazy val romeStore = Factory
      .store(
        merchant,
        locationId = romeId,
        catalogId = romeCatalogId,
        currency = Some(Currency.getInstance("USD")),
      )
      .create

    val paymentProcessorConfig = StripeConfig(
      accountId = "my-account-id",
      publishableKey = "my-publishable-key",
    )

    override lazy val merchant = Factory
      .merchant(
        paymentProcessor = Some(PaymentProcessor.Stripe),
        paymentProcessorConfig = Some(paymentProcessorConfig),
      )
      .create

    implicit val storeContext = StoreContext.fromRecord(romeStore)

    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Open

    lazy val order =
      randomOrder(acceptanceStatus.some)

    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)
  }
}
