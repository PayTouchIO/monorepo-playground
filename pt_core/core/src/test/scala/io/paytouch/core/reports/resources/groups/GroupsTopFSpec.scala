package io.paytouch.core.reports.resources.groups

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._

class GroupsTopFSpec extends GroupsFSpec {

  def action = "top"

  class GroupsTopFSpecContext extends GroupsFSpecContext {
    val bronzeGroupTop = GroupTop(bronze.id, bronze.name, 1000.$$$, 991.65.$$$, 99.400, 1)
    val goldGroupTop = GroupTop(gold.id, gold.name, 16.$$$, -2.89.$$$, 40.6250, 3)
    val silverGroupTop = GroupTop(silver.id, silver.name, 33.$$$, -502.93.$$$, -1474.2424, 5)
  }

  "GET /v1/reports/groups.top" in {

    "with no order_by it should reject the request" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval")
        .addHeader(authorizationHeader) ~> routes ~> check {
        rejection ==== MissingQueryParamRejection("order_by[]")
      }
    }

    "when no items are found" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$emptyParams&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]

        val expectedResult = buildExpectedResultWhenNoInterval[GroupTop](emptyFrom, emptyTo)
        result ==== expectedResult
      }
    }

    "with order by id" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]

        val expectedGroupTops = Seq(
          bronzeGroupTop,
          goldGroupTop,
          silverGroupTop,
        )
        val expectedResult = buildExpectedResultWhenNoInterval(expectedGroupTops.sortBy(_.id.toString): _*)
        result ==== expectedResult
      }
    }

    "with order by name" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=name")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]

        val expectedResult = buildExpectedResultWhenNoInterval(
          bronzeGroupTop,
          goldGroupTop,
          silverGroupTop,
        )
        result ==== expectedResult
      }
    }

    "with order by visit" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=visit&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          silverGroupTop,
          goldGroupTop,
        )
        result ==== expectedResult
      }
    }

    "with order by profit" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=profit&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          bronzeGroupTop,
          goldGroupTop,
        )
        result ==== expectedResult
      }
    }

    "with order by spend" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=spend&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          bronzeGroupTop,
          silverGroupTop,
        )
        result ==== expectedResult
      }
    }

    "with order by margin" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=margin&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          bronzeGroupTop,
          goldGroupTop,
        )
        result ==== expectedResult
      }
    }

    "with location_id" in new GroupsTopFSpecContext {
      Get(s"/v1/reports/groups.top?$defaultParamsNoInterval&order_by[]=id&location_id=${london.id}")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[GroupTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          bronzeGroupTop,
        )
        result ==== expectedResult
      }
    }
  }
}
