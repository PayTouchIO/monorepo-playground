package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.clients.paytouch.core.entities.Table
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class MerchantByIdTableSpec extends MerchantByIdSchemaSpec {

  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    val table = random[Table]
  }

  "GraphQL Schema" should {

    "allow to fetch table information of a merchant" in new SpecContext {

      val query =
        graphql"""
         query FetchMerchant($$merchantId: UUID!, $$tableId: UUID!) {
           merchant: merchantById(id: $$merchantId) {
             table(id: $$tableId) {
                id
             }
           }
         }
       """

      val vars = JObject("merchantId" -> JString(merchant.id.toString), "tableId" -> JString(table.id.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "table": $s } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        table,
      )
    }
  }
}
