package io.paytouch.ordering.graphql.merchantById

import sangria.macros._

import org.json4s.JsonAST.{ JObject, JString }

import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData

class MerchantByIdLocationSpec extends MerchantByIdSchemaSpec {
  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    @scala.annotation.nowarn("msg=Auto-application")
    val location =
      random[Location]

    PtCoreStubData.recordLocation(location)
  }

  "GraphQL Schema" should {

    "allow to fetch location information of a merchant" in new SpecContext {

      val query =
        graphql"""
         query FetchMerchant($$merchantId: UUID!, $$locationId: UUID!) {
           merchant: merchantById(id: $$merchantId) {
             location(id: $$locationId) {
                id
                name
                email
                phone_number
                website
                active
                address {
                  line1
                  line2
                  city
                  state
                  country
                  state_data {
                    country {
                      code
                      name
                    }
                    code
                    name
                  }
                  postal_code
                }
                timezone
                currency
                coordinates { lat, lng }
             }
           }
         }
       """

      val vars = JObject("merchantId" -> JString(merchant.id.toString), "locationId" -> JString(location.id.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "location": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        location,
        fieldsToRemove = Seq("opening_hours", "settings"),
      )
    }
  }
}
