package io.paytouch.ordering.clients.worldpay

import java.util.UUID

import akka.http.scaladsl.model.{ Uri => AkkaUri }

import com.softwaremill.macwire._
import com.softwaremill.sttp._
import com.softwaremill.sttp.testing._

import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.data.model.{ CartRecord, WorldpayConfig }
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, FixturesSupport, MockedRestApi }
import io.paytouch.ordering._

import org.specs2.specification.Scope

import scala.concurrent.{ ExecutionContext, Future }

class WorldpayClientSpec extends FSpec {
  trait ClientSpecContext extends FixturesSupport with Scope with CommonArbitraries {
    lazy val config = WorldpayConfig(
      accountId = "accountId",
      terminalId = "tid",
      acceptorId = "acceptorId",
      accountToken = "token",
    )

    lazy val total = genMonetaryAmount.instance
    lazy val orderId = UUID.randomUUID
    lazy val storeName = genOptString.instance

    implicit def backend: SttpBackend[Id, Nothing]

    lazy val worldpayTransactionEndpointUri = AkkaUri("transacation-endpoint")
      .taggedWith[WorldpayTransactionEndpointUri]
    lazy val worldpayReportingEndpointUri = AkkaUri("reporting-endpoint")
      .taggedWith[WorldpayReportingEndpointUri]
    lazy val worldpayCheckoutUri = AkkaUri("checkout").taggedWith[WorldpayCheckoutUri]
    lazy val worldpayApplicationId = "application-id".taggedWith[WorldpayApplicationId]
    lazy val worldpayReturnUri = AkkaUri("return-uri").taggedWith[WorldpayReturnUri]

    lazy val client = wire[WorldpayClient]

    def stubResponse(resourcePath: String): SttpBackend[Id, Nothing] =
      SttpBackendStub.synchronous.whenRequestMatches(_ => true).thenRespond(loadResource(resourcePath))
  }

}
