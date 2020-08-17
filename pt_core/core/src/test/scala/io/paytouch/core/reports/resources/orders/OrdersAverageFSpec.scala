package io.paytouch.core.reports.resources.orders

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._

class OrdersAverageFSpec extends OrdersFSpec {

  def action = "average"

  import fixtures._

  class OrdersAverageFSpecContext extends OrdersFSpecContext

  "GET /v1/reports/orders.average" in {

    "with no interval" should {
      "with no group by" should {
        val emptyAggregate = OrderAggregate(count = 3)

        val fullAggregate =
          OrderAggregate(
            count = 3,
            profit = Some(-3.73.$$$),
            revenue = Some(5.33.$$$),
            waitingTimeInSeconds = Some(1900),
          )

        assertNoField()

        assertFieldResultWhenNoItems(
          "profit",
          ReportFields(key = None, OrderAggregate(count = 0, profit = Some(0.$$$))),
        )

        assertFieldResult("profit", ReportFields(key = None, emptyAggregate.copy(profit = fullAggregate.profit)))

        assertFieldResult("revenue", ReportFields(key = None, emptyAggregate.copy(revenue = fullAggregate.revenue)))

        assertFieldResult(
          "waiting_time",
          ReportFields(key = None, emptyAggregate.copy(waitingTimeInSeconds = fullAggregate.waitingTimeInSeconds)),
        )

        assertAllFieldsResult(ReportFields(key = None, fullAggregate))
      }

      "with group" should {
        assertGroupByResult(
          "payment_type",
          ReportFields(
            key = OrderPaymentType.Cash,
            OrderAggregate(
              count = 1,
              profit = Some(-0.7.$$$),
              revenue = Some(1.$$$),
              waitingTimeInSeconds = Some(1800),
            ),
          ),
          ReportFields(
            key = OrderPaymentType.CreditCard,
            OrderAggregate(
              count = 1,
              profit = Some(-2.3.$$$),
              revenue = Some(10.$$$),
              waitingTimeInSeconds = Some(3600),
            ),
          ),
          ReportFields(
            key = OrderPaymentType.DebitCard,
            OrderAggregate(
              count = 1,
              profit = Some(-8.20.$$$),
              revenue = Some(5.$$$),
              waitingTimeInSeconds = Some(300),
            ),
          ),
        )

        assertGroupByResult(
          "order_type",
          ReportFields(
            key = OrderType.DineIn,
            OrderAggregate(
              count = 2,
              profit = Some(-5.25.$$$),
              revenue = Some(7.5.$$$),
              waitingTimeInSeconds = Some(1950),
            ),
          ),
          ReportFields(
            key = OrderType.InStore,
            OrderAggregate(
              count = 1,
              profit = Some(-0.7.$$$),
              revenue = Some(1.$$$),
              waitingTimeInSeconds = Some(1800),
            ),
          ),
        )

        assertGroupByResult(
          "source_type",
          ReportFields(
            key = Source.Register,
            OrderAggregate(
              count = 1,
              profit = Some(-2.30.$$$),
              revenue = Some(10.$$$),
              waitingTimeInSeconds = Some(3600),
            ),
          ),
          ReportFields(
            key = Source.Storefront,
            OrderAggregate(
              count = 2,
              profit = Some(-4.45.$$$),
              revenue = Some(3.$$$),
              waitingTimeInSeconds = Some(1050),
            ),
          ),
        )

        assertGroupByResult(
          "feedback",
          ReportFields(Some("3"), OrderAggregate(1, Some(-8.20.$$$), Some(5.00.$$$), Some(300))),
          ReportFields(Some("5"), OrderAggregate(1, Some(-2.30.$$$), Some(10.$$$), Some(3600))),
          ReportFields(None, OrderAggregate(1, Some(-0.7.$$$), Some(1.$$$), Some(1800))),
        )
      }
    }

    "with interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field profit" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=profit")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      profit = Some(-3.73.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      profit = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with field revenue" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=revenue")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      revenue = Some(5.33.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      revenue = Some(0.$$$),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with field waiting time" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=waiting_time")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 3,
                      waitingTimeInSeconds = Some(1900),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(
                  ReportFields(
                    None,
                    OrderAggregate(
                      count = 0,
                      waitingTimeInSeconds = Some(0),
                    ),
                  ),
                ),
              ),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(ReportFields(None, OrderAggregate(3, Some(-3.73.$$$), Some(5.33.$$$), Some(1900)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by payment type" should {
        "with all the fields" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=payment_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(OrderPaymentType.Cash, OrderAggregate(1, Some(-0.7.$$$), Some(1.$$$), Some(1800))),
                  ReportFields(
                    OrderPaymentType.CreditCard,
                    OrderAggregate(1, Some(-2.3.$$$), Some(10.$$$), Some(3600)),
                  ),
                  ReportFields(OrderPaymentType.DebitCard, OrderAggregate(1, Some(-8.2.$$$), Some(5.$$$), Some(300))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by order type" should {
        "with all the fields" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=order_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(OrderType.DineIn, OrderAggregate(2, Some(-5.25.$$$), Some(7.50.$$$), Some(1950))),
                  ReportFields(OrderType.InStore, OrderAggregate(1, Some(-0.7.$$$), Some(1.$$$), Some(1800))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by source type" should {
        "with all the fields" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=$allFieldsParams&&group_by=source_type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(Source.Register, OrderAggregate(1, Some(-2.3.$$$), Some(10.$$$), Some(3600))),
                  ReportFields(Source.Storefront, OrderAggregate(2, Some(-4.45.$$$), Some(3.$$$), Some(1050))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by feedback" should {
        "with all the fields" in new OrdersAverageFSpecContext {
          Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=$allFieldsParams&&group_by=feedback")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(Some("3"), OrderAggregate(1, Some(-8.2.$$$), Some(5.$$$), Some(300))),
                  ReportFields(Some("5"), OrderAggregate(1, Some(-2.3.$$$), Some(10.$$$), Some(3600))),
                  ReportFields(None, OrderAggregate(1, Some(-0.7.$$$), Some(1.$$$), Some(1800))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderAggregate(0, Some(0.$$$), Some(0.$$$), Some(0)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location_id" should {
      "return the correct data" in new OrdersAverageFSpecContext {
        Get(s"/v1/reports/orders.average?$defaultParamsWithInterval&field[]=profit&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportFields[OrderAggregate]]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 1,
                    profit = Some(-8.2.$$$),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(21), end.plusDays(21)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                  ),
                ),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              List(
                ReportFields(
                  None,
                  OrderAggregate(
                    count = 0,
                    profit = Some(0.$$$),
                  ),
                ),
              ),
            ),
          )
          result ==== expectedResult
        }
      }
    }
  }
}
