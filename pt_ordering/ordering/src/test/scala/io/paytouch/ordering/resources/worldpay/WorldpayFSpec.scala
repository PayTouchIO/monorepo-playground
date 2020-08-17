package io.paytouch.ordering.resources.worldpay

import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.clients.paytouch.core.entities.Order
import io.paytouch.ordering.data.model.{ WorldpayConfig, WorldpayPaymentType }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.{ CartStatus, PaymentProcessor }
import io.paytouch.ordering.entities.worldpay._
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MultipleLocationFixtures }
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

abstract class WorldpayFSpec extends FSpec with CommonArbitraries {

  abstract class WorldpayFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val worldpayPaymentDao = daos.worldpayPaymentDao
    val cartDao = daos.cartDao

    override lazy val romeStore = Factory
      .store(
        merchant,
        locationId = romeId,
        catalogId = romeCatalogId,
        currency = Some(Currency.getInstance("USD")),
      )
      .create

    val paymentProcessorConfig = WorldpayConfig(
      accountId = "accountId",
      terminalId = "tid",
      acceptorId = "acceptorId",
      accountToken = "token",
    )

    override lazy val merchant = Factory
      .merchant(
        paymentProcessor = Some(PaymentProcessor.Worldpay),
        paymentProcessorConfig = Some(paymentProcessorConfig),
      )
      .create

    implicit val storeContext = StoreContext.fromRecord(romeStore)

    lazy val acceptanceStatus: AcceptanceStatus = AcceptanceStatus.Open

    lazy val order =
      randomOrder(acceptanceStatus.some)

    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    PtCoreStubData.recordOrder(order)

    def assertWorldpayPayment(
        transactionSetupId: String,
        status: WorldpayPaymentStatus,
        cartId: Option[UUID] = None,
      ) = {
      val maybePayment = worldpayPaymentDao.findByTransactionSetupId(transactionSetupId).await
      maybePayment must beSome
      val payment = maybePayment.get

      payment.status ==== status

      if (cartId.isDefined) {
        payment.objectId ==== cartId.get
        payment.objectType ==== WorldpayPaymentType.Cart
      }
    }
  }
}
