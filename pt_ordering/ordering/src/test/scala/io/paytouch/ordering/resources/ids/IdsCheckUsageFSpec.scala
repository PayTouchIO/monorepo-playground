package io.paytouch.ordering.resources.ids

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.{ FSpec, MultipleLocationFixtures, FixtureDaoFactory => Factory }

class IdsCheckUsageFSpec extends FSpec {

  abstract class IdsCheckUsageFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val competitorCatalogId = UUID.randomUUID
    Factory.store(competitor, locationId = UUID.randomUUID, catalogId = competitorCatalogId).create

    val myCatalogId = UUID.randomUUID
    Factory.store(merchant, locationId = romeId, catalogId = myCatalogId).create

    val unusedCatalogId = UUID.randomUUID

    private val catalogIds = Seq(competitorCatalogId, myCatalogId, unusedCatalogId)
    val ids = Ids(catalogIds = catalogIds)
  }

  "POST /v1/ids.check_usage?merchant_id=<>" in {
    "if request has valid token" in {
      "return the merchant" in new IdsCheckUsageFSpecContext {
        Post(s"/v1/ids.check_usage?merchant_id=${merchant.id}", ids)
          .addHeader(coreAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[IdsUsage]].data
          entity ==== IdsUsage(
            accessible = Ids(catalogIds = Seq(myCatalogId)),
            notUsed = Ids(catalogIds = Seq(unusedCatalogId)),
            nonAccessible = Ids(catalogIds = Seq(competitorCatalogId)),
          )
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new IdsCheckUsageFSpecContext {
        Post(s"/v1/ids.check_usage?merchant_id=${merchant.id}", ids)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
