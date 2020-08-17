package io.paytouch.ordering.resources.merchants

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.{ CommonArbitraries, FixtureDaoFactory => Factory }

class MerchantsUpdateFSpec extends MerchantsFSpec with CommonArbitraries {

  abstract class MerchantsUpdateFSpecContext extends MerchantResourceFSpecContext {
    val update = random[MerchantUpdate]
  }

  "POST /v1/merchants.update" in {
    "if request has valid token" in {

      "if request is valid" should {
        "update a merchant" in new MerchantsUpdateFSpecContext {
          Post("/v1/merchants.update", update).addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val entity = responseAs[ApiResponse[Merchant]].data

            assertUpdate(merchant.id, update)
            assertResponseById(merchant.id, entity)
          }
        }
      }

      "if url slug is already taken" should {
        "reject the request" in new MerchantsUpdateFSpecContext {
          val invalidUpdate = update.copy(urlSlug = Some(competitor.urlSlug))
          Post("/v1/merchants.update", invalidUpdate).addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)

            assertErrorCode("UrlSlugAlreadyTaken")
          }

        }
      }

      "if a merchant does not exists" should {
        "reject the request" in new MerchantsUpdateFSpecContext {
          storeDao.deleteByIds(stores.map(_.id)).await
          merchantDao.deleteById(merchant.id).await

          Post("/v1/merchants.update", update).addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertErrorCode("NonAccessibleMerchantIds")
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new MerchantsUpdateFSpecContext {
        Post("/v1/merchants.update", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
