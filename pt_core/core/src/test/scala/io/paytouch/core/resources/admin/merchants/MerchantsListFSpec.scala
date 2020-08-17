package io.paytouch.core.resources.admin.merchants

import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.BusinessType
import io.paytouch.core.entities.{ Merchant => MerchantEntity, _ }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.utils.MockedRestApi

class MerchantsListFSpec extends MerchantsFSpec with LazyLogging {
  abstract class MerchantsListFSpecContext extends MerchantResourceFSpecContext {
    // randomize email to avoid conflicts, don't use random text to avoid flakiness in search (which searches owner's email address too)
    val domain = s"example$randomNumericString.com"

    val merchant1 = Factory
      .merchant(
        name = Some("Carlo's Coffee"),
        businessType = Some(BusinessType.Restaurant),
      )
      .create
    val owner1 = Factory.user(merchant1, isOwner = Some(true), email = Some(s"speedmaster9999@$domain")).create
    val location1 = Factory.location(merchant1).create

    val merchant2 = Factory
      .merchant(
        name = Some("Gabriele's Guanciale"),
        businessType = Some(BusinessType.Retail),
      )
      .create
    val owner2 = Factory.user(merchant2, isOwner = Some(true), email = Some(s"gabriele@$domain")).create
    val worker = Factory.user(merchant2, isOwner = Some(false), email = Some(s"speedmaster8888@$domain")).create
    val location2 = Factory.location(merchant2).create

    val merchant3 = Factory
      .merchant(
        name = Some("Francesco's Flans"),
        businessType = Some(BusinessType.Restaurant),
      )
      .create
    val owner3 = Factory.user(merchant3, isOwner = Some(true), email = Some(s"francesco@$domain")).create
    val location3 = Factory.location(merchant3).create

    // As the tests run concurrently there are multiple merchants created, and
    // we have no way to scope them. So we filter the response so it only
    // contains merchants we created for this test run.
    val merchantIds = Seq(merchant1.id, merchant2.id, merchant3.id)
    val merchantIdsFilter = s"ids[]=${merchantIds.mkString(",")}"
  }

  "GET /v1/admin/merchants.list" in {
    "if request has valid token" in {
      "with no parameter" should {
        "return a paginated list of all merchants sorted by name" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter").addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)
            merchants.map(_.id) ==== Seq(merchant1.id, merchant3.id, merchant2.id)
          }
        }
      }

      "with order_by" should {
        "return a paginated list of all merchants sorted by reverse name" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&sort_by[]=-business_name")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)
            merchants.map(_.id) ==== Seq(merchant2.id, merchant3.id, merchant1.id)
          }
        }
      }

      "with expand[]=owners" should {
        "return a paginated list of all merchants with ownerUser expanded" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&expand[]=owners")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)

            merchants(0).id ==== merchant1.id
            merchants(0).ownerUser must beSome
            merchants(0).ownerUser.get.id ==== owner1.id

            merchants(1).id ==== merchant3.id
            merchants(1).ownerUser must beSome
            merchants(1).ownerUser.get.id ==== owner3.id

            merchants(2).id ==== merchant2.id
            merchants(2).ownerUser must beSome
            merchants(2).ownerUser.get.id ==== owner2.id
          }
        }
      }

      "with expand[]=locations" should {
        "return a paginated list of all merchants with locations expanded" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&expand[]=locations")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)

            merchants(0).id ==== merchant1.id
            merchants(0).locations must beSome
            merchants(0).locations.get(0).id ==== location1.id

            merchants(1).id ==== merchant3.id
            merchants(1).locations must beSome
            merchants(1).locations.get(0).id ==== location3.id

            merchants(2).id ==== merchant2.id
            merchants(2).locations must beSome
            merchants(2).locations.get(0).id ==== location2.id
          }
        }
      }

      "filtered by business type" should {
        "return a paginated list of all merchants sorted by name matching the business type parameter" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&business_type=retail")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)
            merchants.map(_.id) ==== Seq(merchant2.id)
          }
        }
      }

      "filtered by q parameter" should {
        "return a paginated list of all merchants sorted by name matching the q parameter to the name" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&q=an")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)
            merchants.map(_.id) ==== Seq(merchant3.id, merchant2.id)
          }
        }

        "return a paginated list of all merchants sorted by name matching the q parameter to the owner email" in new MerchantsListFSpecContext {
          Get(s"/v1/admin/merchants.list?$merchantIdsFilter&q=speedmaster")
            .addHeader(adminAuthorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponseWithMetadata[Seq[MerchantEntity]]]
            val merchants = response.data.filter(merchantIds contains _.id)
            merchants.map(_.id) ==== Seq(merchant1.id)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new MerchantsListFSpecContext {
        Get(s"/v1/admin/merchants.list")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
