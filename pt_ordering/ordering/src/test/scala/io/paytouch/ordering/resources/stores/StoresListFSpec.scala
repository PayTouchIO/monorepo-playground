package io.paytouch.ordering.resources.stores

import akka.http.scaladsl.server.AuthenticationFailedRejection

import io.paytouch.ordering.data.model.{ EkashuConfig, JetdirectConfig, WorldpayConfig }
import io.paytouch.ordering.entities.{ PaginatedApiResponse, Store }
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, Generators }

class StoresListFSpec extends StoresFSpec {
  "GET /v1/stores.list" in {
    "if request has valid token" in {
      "merchant payment processor = ekashu" in {
        trait FSpecContext extends StoreResourceFSpecContext with Generators {
          override lazy val merchant = Factory
            .merchant(
              paymentProcessor = Some(PaymentProcessor.Ekashu),
              paymentProcessorConfig = Some(
                EkashuConfig(
                  sellerId = genString.instance,
                  sellerKey = genString.instance,
                  hashKey = genString.instance,
                ),
              ),
            )
            .create
        }

        "return a paginated list of all stores" in new FSpecContext {
          Get("/v1/stores.list").addHeader(userAuthorizationHeader) ~> routes ~> check {

            val entities = responseAs[PaginatedApiResponse[Seq[Store]]].data
            entities.map(_.id) ==== Seq(londonStore.id, romeStore.id)
            assertResponse(londonStore, entities.find(_.id == londonStore.id).get)
            assertResponse(romeStore, entities.find(_.id == romeStore.id).get)
          }
        }
      }

      "merchant payment processor = jetdirect" in {
        trait FSpecContext extends StoreResourceFSpecContext with Generators {
          override lazy val merchant = Factory
            .merchant(
              paymentProcessor = Some(PaymentProcessor.Jetdirect),
              paymentProcessorConfig = Some(
                JetdirectConfig(
                  merchantId = genString.instance,
                  terminalId = genString.instance,
                  key = genString.instance,
                  securityToken = genString.instance,
                ),
              ),
            )
            .create
        }

        "return a paginated list of all stores" in new FSpecContext {
          Get("/v1/stores.list").addHeader(userAuthorizationHeader) ~> routes ~> check {

            val entities = responseAs[PaginatedApiResponse[Seq[Store]]].data
            entities.map(_.id) ==== Seq(londonStore.id, romeStore.id)
            assertResponse(londonStore, entities.find(_.id == londonStore.id).get)
            assertResponse(romeStore, entities.find(_.id == romeStore.id).get)
          }
        }
      }

      "merchant payment processor = worldpay" in {
        trait FSpecContext extends StoreResourceFSpecContext with Generators {
          override lazy val merchant = Factory
            .merchant(
              paymentProcessor = Some(PaymentProcessor.Worldpay),
              paymentProcessorConfig = Some(
                WorldpayConfig(
                  accountId = genString.instance,
                  terminalId = genString.instance,
                  acceptorId = genString.instance,
                  accountToken = genString.instance,
                ),
              ),
            )
            .create
        }

        "return a paginated list of all stores" in new FSpecContext {
          Get("/v1/stores.list").addHeader(userAuthorizationHeader) ~> routes ~> check {

            val entities = responseAs[PaginatedApiResponse[Seq[Store]]].data
            entities.map(_.id) ==== Seq(londonStore.id, romeStore.id)
            assertResponse(londonStore, entities.find(_.id == londonStore.id).get)
            assertResponse(romeStore, entities.find(_.id == romeStore.id).get)
          }
        }
      }
    }

    "if request has an invalid token" in {

      "reject the request" in new StoreResourceFSpecContext {
        Get(s"/v1/stores.list").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }

}
