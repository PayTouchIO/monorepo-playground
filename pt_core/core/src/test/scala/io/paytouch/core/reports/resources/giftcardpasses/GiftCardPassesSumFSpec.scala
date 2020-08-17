package io.paytouch.core.reports.resources.giftcardpasses

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ ReportFields, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class GiftCardPassesSumFSpec extends GiftCardPassesFSpec {

  def action = "sum"

  import fixtures._

  class GiftCardPassesSumFSpecContext extends GiftCardPassesFSpecContext

  "GET /v1/reports/gift_card_passes.sum" in {

    "with no interval" should {
      "with no group by" should {
        val emptyAggregate = GiftCardPassAggregate(count = 4)

        val fullAggregate = GiftCardPassAggregate(
          count = 4,
          customers = Some(4),
          total = Some(210.$$$),
          redeemed = Some(55.$$$),
          unused = Some(155.$$$),
        )

        assertNoField()

        assertFieldResultWhenNoItems(
          "customers",
          ReportFields(key = None, GiftCardPassAggregate(count = 0, customers = Some(0))),
        )

        assertFieldResult("total", ReportFields(key = None, emptyAggregate.copy(total = fullAggregate.total)))

        assertFieldResult("redeemed", ReportFields(key = None, emptyAggregate.copy(redeemed = fullAggregate.redeemed)))

        assertFieldResult("unused", ReportFields(key = None, emptyAggregate.copy(unused = fullAggregate.unused)))

        assertAllFieldsResult(ReportFields(key = None, fullAggregate))
      }

      "with group by" should {
        assertGroupByResult(
          "value",
          ReportFields(
            key = Some("100.00"),
            GiftCardPassAggregate(
              count = 1,
              customers = Some(1),
              total = Some(100.$$$),
              redeemed = Some(0.$$$),
              unused = Some(100.$$$),
            ),
          ),
          ReportFields(
            key = Some("50.00"),
            GiftCardPassAggregate(
              count = 1,
              customers = Some(1),
              total = Some(50.$$$),
              redeemed = Some(30.$$$),
              unused = Some(20.$$$),
            ),
          ),
          ReportFields(
            key = Some("custom"),
            GiftCardPassAggregate(
              count = 2,
              customers = Some(2),
              total = Some(60.$$$),
              redeemed = Some(25.$$$),
              unused = Some(35.$$$),
            ),
          ),
        )

      }
    }

    "with interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in new GiftCardPassesSumFSpecContext {
          Get(s"/v1/reports/gift_card_passes.sum?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with all fields" in new GiftCardPassesSumFSpecContext {
          Get(s"/v1/reports/gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[GiftCardPassAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 0,
                      customers = Some(0),
                      total = Some(0.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 2,
                      customers = Some(2),
                      total = Some(70.$$$),
                      redeemed = Some(25.$$$),
                      unused = Some(45.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(40.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(40.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(100.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(100.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 0,
                      customers = Some(0),
                      total = Some(0.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by value" should {
        "with all the fields" in new GiftCardPassesSumFSpecContext {
          Get(s"/v1/reports/gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=value")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[GiftCardPassAggregate]]]

            val emptyAggregate = GiftCardPassAggregate(
              count = 0,
              customers = Some(0),
              total = Some(0.$$$),
              redeemed = Some(0.$$$),
              unused = Some(0.$$$),
            )

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, emptyAggregate))),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    Some("50.00"),
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(50.$$$),
                      redeemed = Some(25.$$$),
                      unused = Some(25.$$$),
                    ),
                  ),
                  ReportFields(
                    Some("custom"),
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(20.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(20.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    Some("custom"),
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(40.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(40.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    Some("100.00"),
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(100.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(100.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, emptyAggregate)),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with location_id" should {
        "returns correct data" in new GiftCardPassesSumFSpecContext {
          Get(
            s"/v1/reports/gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${willow1.id}",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[GiftCardPassAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 0,
                      customers = Some(0),
                      total = Some(0.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(50.$$$),
                      redeemed = Some(25.$$$),
                      unused = Some(25.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 1,
                      customers = Some(1),
                      total = Some(40.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(40.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 0,
                      customers = Some(0),
                      total = Some(0.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    GiftCardPassAggregate(
                      count = 0,
                      customers = Some(0),
                      total = Some(0.$$$),
                      redeemed = Some(0.$$$),
                      unused = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with a non-accessible location" should {
          "reject the request" in new GiftCardPassesSumFSpecContext {
            val newYork = Factory.location(merchant).create

            Get(
              s"/v1/reports/gift_card_passes.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${newYork.id}",
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  }
}
