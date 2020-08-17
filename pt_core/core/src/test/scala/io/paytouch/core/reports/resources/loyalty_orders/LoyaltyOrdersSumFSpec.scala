package io.paytouch.core.reports.resources.loyalty_orders

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._
import io.paytouch.core.data.model.enums.OrderType

class LoyaltyOrdersSumFSpec extends LoyaltyOrdersFSpec {

  def action = "sum"

  import fixtures._

  class LoyaltyOrdersSumFSpecContext extends LoyaltyOrdersFSpecContext

  "GET /v1/reports/loyalty_orders.sum" in {
    "with no interval" should {
      "with no group by" should {
        "with all the fields" in new LoyaltyOrdersSumFSpecContext {
          Get(s"/v1/reports/loyalty_orders.sum?$defaultParamsNoInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[LoyaltyOrdersAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, LoyaltyOrdersAggregate(count = 4, amount = Some(25.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by order type" should {
        "with all the fields" in new LoyaltyOrdersSumFSpecContext {
          Get(s"/v1/reports/loyalty_orders.sum?$defaultParamsNoInterval&field[]=$allFieldsParams&group_by=type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[LoyaltyOrdersAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = OrderType.DeliveryRestaurant, LoyaltyOrdersAggregate(count = 1, amount = Some(3.$$$))),
              ReportFields(key = OrderType.InStore, LoyaltyOrdersAggregate(count = 2, amount = Some(17.$$$))),
              ReportFields(key = OrderType.TakeOut, LoyaltyOrdersAggregate(count = 1, amount = Some(5.$$$))),
            )
            result ==== expectedResult
          }
        }
      }
    }
  }
}
