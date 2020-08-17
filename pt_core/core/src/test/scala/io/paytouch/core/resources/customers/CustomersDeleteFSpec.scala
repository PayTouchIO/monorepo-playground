package io.paytouch.core.resources.customers

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.Ids
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CustomersDeleteFSpec extends CustomersFSpec {

  abstract class CustomerDeleteFSpecContext extends CustomerResourceFSpecContext

  "POST /v1/customers.delete" in {
    "if request has valid token" in {

      "delete existing global customer - merchant association" in new CustomerDeleteFSpecContext {
        val competitor = Factory.merchant.create
        val globalCustomer = Factory.globalCustomer().create

        Factory.customerMerchant(merchant, globalCustomer).create
        Factory.customerMerchant(competitor, globalCustomer).create

        Post(s"/v1/customers.delete", Ids(ids = Seq(globalCustomer.id)))
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          globalCustomerDao.findById(globalCustomer.id).await ==== Some(globalCustomer)
          customerMerchantDao.findByIdAndMerchantId(globalCustomer.id, merchant.id).await ==== None
          customerMerchantDao.findByIdAndMerchantId(globalCustomer.id, competitor.id).await.isDefined should beTrue
        }
      }

      "return ok but do nothing if merchant is already not associated to the global customer" in new CustomerDeleteFSpecContext {
        val competitor = Factory.merchant.create
        val globalCustomer = Factory.globalCustomer(merchant = Some(competitor)).create

        Post(s"/v1/customers.delete", Ids(ids = Seq(globalCustomer.id)))
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)

          globalCustomerDao.findById(globalCustomer.id).await ==== Some(globalCustomer)
          customerMerchantDao.findByIdAndMerchantId(globalCustomer.id, competitor.id).await.isDefined should beTrue
        }
      }
    }
  }
}
