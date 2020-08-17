package io.paytouch.core.resources.admin.merchants

import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.BusinessType
import io.paytouch.core.entities.{ Merchant => MerchantEntity, _ }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.utils.MockedRestApi

class MerchantsGetFSpec extends MerchantsFSpec with LazyLogging {
  abstract class MerchantsGetFSpecContext extends MerchantResourceFSpecContext {
    val merchant1 = Factory
      .merchant(
        name = Some("Carlo's Coffee"),
        businessType = Some(BusinessType.Restaurant),
      )
      .create
    val owner1 = Factory.user(merchant1, isOwner = Some(true)).create
    val location1 = Factory.location(merchant1).create
  }

  "GET /v1/admin/merchants.get" in {
    "if request has valid token" in {
      "return a merchant with everything expanded" in new MerchantsGetFSpecContext {
        Get(s"/v1/admin/merchants.get?merchant_id=${merchant1.id}")
          .addHeader(adminAuthorizationHeader) ~> routes ~> check {
          val response = responseAs[ApiResponse[MerchantEntity]]
          response.data.id ==== merchant1.id

          response.data.ownerUser must beSome
          response.data.ownerUser.get.id ==== owner1.id

          response.data.locations must beSome
          response.data.locations.get(0).id ==== location1.id
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsGetFSpecContext {
        Get(s"/v1/admin/merchants.get?merchant_id=${merchant1.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
