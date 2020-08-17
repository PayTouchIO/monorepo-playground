package io.paytouch.ordering.graphql.merchantById

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.stubs.PtCoreStubData
import org.json4s.JsonAST.{ JObject, JString }
import sangria.macros._

class MerchantByIdOrderSpec extends MerchantByIdSchemaSpec {

  abstract class SpecContext extends MerchantByIdSchemaSpecContext {
    val merchantContext = Merchant.fromRecord(merchant)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCoreMerchant(merchant.id)

    val order = randomOrder()
    PtCoreStubData.recordOrder(order)
  }

  "GraphQL Schema" should {

    "allow to fetch open order information of a merchant table" in new SpecContext {

      val query =
        graphql"""
          query FetchMerchant($$merchantId: UUID!, $$orderId: UUID!) {
            merchant: merchantById(id: $$merchantId) {
              order(id: $$orderId) {
                id
                number
                status
                payment_status
                items {
                  product_name
                  quantity
                  payment_status
                  variant_options {
                    option_name
                    option_type_name
                    position
                  }
                  modifier_options {
                    option_name
                    option_type_name
                    type
                    price {
                      amount
                      currency
                    }
                    quantity
                  }
                  calculated_price {
                    amount
                    currency
                  }
                  total_price {
                    amount
                    currency
                  }
                  tax {
                    amount
                    currency
                  }
                  tax_rates {
                    name
                    value
                    amount
                  }
                }
                bundles {
                  bundle_order_item_id
                  bundle_sets {
                    position
                    bundle_options {
                      article_order_item_id
                      position
                      price_adjustment
                    }
                  }
                }
                subtotal {
                  amount
                  currency
                }
                tax {
                  amount
                  currency
                }
                tax_rates {
                  name
                  value
                  amount
                }
                tip {
                  amount
                  currency
                }
                total {
                  amount
                  currency
                }
                payment_transactions {
                  type
                  payment_type
                  payment_details {
                    amount
                    currency
                  }
                  paid_at
                  order_item_ids
                }
                online_order_attribute {
                  id
                  acceptance_status
                  rejection_reason
                  prepare_by_time
                  prepare_by_date_time
                  estimated_prep_time_in_mins
                  rejected_at
                  accepted_at
                  estimated_ready_at
                  estimated_delivered_at
                  cancellation_reason
                }
                created_at
                updated_at
              }
            }
          }
       """

      val vars = JObject(
        "merchantId" -> JString(merchant.id.toString),
        "orderId" -> JString(order.id.toString),
      )
      val result = executeQuery(query, vars = vars)

      val wrapper = { s: String => s"""{ "merchant" : { "order": $s } }""" }

      val ignoreFields = Seq("items", "bundles", "payment_transactions", "tax_rates", "completed_at", "location")
      addRemove(result, fieldsToRemove = ignoreFields) ==== parseAsEntityWithWrapper(
        wrapper,
        order,
        fieldsToRemove = ignoreFields,
      )
    }
  }
}
