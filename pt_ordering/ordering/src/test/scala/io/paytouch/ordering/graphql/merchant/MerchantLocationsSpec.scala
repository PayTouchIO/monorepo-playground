package io.paytouch.ordering.graphql.merchant

import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class MerchantLocationsSpec extends MerchantSchemaSpec {
  abstract class MerchantLocationSpecContext extends MerchantSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)
  }

  "GraphQL Schema" should {
    "allow to fetch location information of a merchant" in new MerchantLocationSpecContext {
      @scala.annotation.nowarn("msg=Auto-application")
      val expectedLocation =
        random[Location]

      PtCoreStubData.recordLocation(expectedLocation)

      val query =
        graphql"""
         query FetchMerchantLocations($$slug: String!) {
           merchant(slug: $$slug) {
             locations {
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

      val vars = JObject("merchant_slug" -> JString(merchant.urlSlug), "slug" -> JString(merchant.urlSlug))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "locations": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        Seq(expectedLocation),
        fieldsToRemove = Seq("opening_hours", "settings"),
      )
    }
  }
}
