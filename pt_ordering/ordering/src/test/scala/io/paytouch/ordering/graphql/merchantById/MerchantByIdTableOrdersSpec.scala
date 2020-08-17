package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class MerchantByIdTableOrdersSpec extends MerchantByIdSchemaSpec {

  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    val table = random[Table]

    val order1 = randomOrder()
    val order2 = randomOrder()
    val order3 = randomOrder()
    PtCoreStubData.recordOrders(Seq(order1, order2, order3))
  }

  "GraphQL Schema" should {

    "allow to fetch open order information of a merchant table" in new SpecContext {

      val query =
        graphql"""
          query FetchMerchant($$merchantId: UUID!, $$tableId: UUID!) {
            merchant: merchantById(id: $$merchantId) {
              table(id: $$tableId) {
                orders {
                  id
                }
              }
            }
          }
       """

      val vars = JObject("merchantId" -> JString(merchant.id.toString), "tableId" -> JString(table.id.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "table": { "orders": $s } } }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        Seq(order1, order2, order3),
        fieldsToInclude = Seq("id"),
      )
    }
  }
}
