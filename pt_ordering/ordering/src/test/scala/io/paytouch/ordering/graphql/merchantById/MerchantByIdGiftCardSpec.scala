package io.paytouch.ordering.graphql.merchantById

import sangria.macros._

import org.json4s.JsonAST.{ JObject, JString }

import io.paytouch.ordering.clients.paytouch.core.entities.GiftCard
import io.paytouch.ordering.entities.{ Merchant, MonetaryAmount }
import io.paytouch.ordering.stubs.PtCoreStubData

class MerchantByIdGiftCardSpec extends MerchantByIdSchemaSpec {
  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    @scala.annotation.nowarn("msg=Auto-application")
    val giftCard =
      random[GiftCard].copy(amounts = Seq(random[MonetaryAmount]))

    PtCoreStubData.recordGiftCard(giftCard)
  }

  "GraphQL Schema" should {
    "allow to fetch gift card information of a merchant" in new SpecContext {
      val query =
        graphql"""
          query FetchMerchant($$merchantId: UUID!) {
            merchant: merchantById(id: $$merchantId) {
              gift_card {
                id
                amounts {
                  amount
                  currency
                }
                business_name
                template_details
                template_created
                active
              }
            }
         }
       """

      val vars = JObject("merchantId" -> JString(merchant.id.toString))
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "gift_card" : $s} }""" }
      result ==== parseAsEntityWithWrapper(
        wrapper,
        giftCard,
        fieldsToRemove = Seq(
          "avatar_image_urls",
          "product",
          "created_at",
          "updated_at",
        ),
      )
    }
  }
}
