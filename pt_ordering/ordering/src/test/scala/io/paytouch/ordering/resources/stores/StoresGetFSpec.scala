package io.paytouch.ordering.resources.stores

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.ordering.entities._
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

class StoresGetFSpec extends StoresFSpec {

  "GET /v1/stores.get?store_id=<store-id>" in {
    "if request has valid token" in {

      "if the store belongs to the location" should {
        "return a store" in new StoreResourceFSpecContext {
          val store = londonStore

          Get(s"/v1/stores.get?store_id=${store.id}").addHeader(userAuthorizationHeader) ~> routes ~> check {
            val entity = responseAs[ApiResponse[Store]].data
            assertResponse(store, entity)
          }
        }
      }

      "if the store does not belong to the location" should {
        "return 404" in new StoreResourceFSpecContext {

          Get(s"/v1/stores.get?store_id=${competitorStore.id}")
            .addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new StoreResourceFSpecContext {
        val storeId = UUID.randomUUID
        Get(s"/v1/stores.get?store_id=$storeId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
