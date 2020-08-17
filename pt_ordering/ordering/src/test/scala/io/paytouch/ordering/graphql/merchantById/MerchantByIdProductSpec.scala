package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class MerchantByIdProductSpec extends MerchantByIdSchemaSpec {
  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    @scala.annotation.nowarn("msg=Auto-application")
    val product =
      random[Product]

    PtCoreStubData.recordProduct(product)
  }

  "GraphQL Schema" should {

    "allow to fetch product information of a merchant table" in new SpecContext {

      val query =
        graphql"""
          query FetchMerchant($$merchantId: UUID!, $$productId: UUID!) {
            merchant: merchantById(id: $$merchantId) {
              product(id: $$productId) {
                id
              }
            }
          }
       """

      val vars = JObject(
        "merchantId" -> JString(merchant.id.toString),
        "productId" -> JString(product.id.toString),
      )
      val result = executeQuery(query, vars = vars)
      result ==== fromJsonStringToJson(s"""{ "data": { "merchant" : { "product": { "id": "${product.id}"} } } }""")
    }
  }
}
