package io.paytouch.core.reports.resources.reward_redemptions

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._

class RewardRedemptionsSumFSpec extends RewardRedemptionsFSpec {

  def action = "sum"

  import fixtures._

  class RewardRedemptionsSumFSpecContext extends RewardRedemptionsFSpecContext

  "GET /v1/reports/reward_redemptions.sum" in {
    "with no interval" should {
      "with no group by" should {
        "with all the fields" in new RewardRedemptionsSumFSpecContext {
          Get(s"/v1/reports/reward_redemptions.sum?$defaultParamsNoInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[RewardRedemptionsAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, RewardRedemptionsAggregate(count = 2, value = Some(20.$$$))),
            )
            result ==== expectedResult
          }
        }
      }
    }
  }
}
