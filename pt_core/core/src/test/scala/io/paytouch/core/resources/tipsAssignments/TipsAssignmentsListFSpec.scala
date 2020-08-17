package io.paytouch.core.resources.tipsAssignments

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.data.model.StatusTransition
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ TipsAssignment => TipsAssignmentEntity, _ }
import io.paytouch.core.entities.enums.HandledVia
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TipsAssignmentsListFSpec extends TipsAssignmentsFSpec {
  trait Fixtures extends TipsAssignmentResourceFSpecContext {
    val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

    val cashDrawer = Factory.cashDrawer(user, london).create
    val cashDrawerActivity = Factory.cashDrawerActivity(cashDrawer).create
    val order = Factory.order(merchant, Some(london)).create

    val tipsAssignment1 = Factory
      .tipsAssignment(
        merchant,
        london,
        user = Some(user),
        order = Some(order),
        handledVia = Some(HandledVia.Unhandled),
        handledViaCashDrawerActivity = Some(cashDrawerActivity),
        overrideNow = Some(now.minusDays(1)),
      )
      .create

    val tipsAssignment2 = Factory
      .tipsAssignment(
        merchant,
        rome,
        overrideNow = Some(now),
      )
      .create

    val tipsAssignment3 = Factory
      .tipsAssignment(
        merchant,
        london,
        handledVia = Some(HandledVia.TipsDistributed),
        overrideNow = Some(now.plusDays(1)),
      )
      .create
  }

  "GET /v1/tips_assignments.list" in {
    "if request has valid token" in {
      "with no parameters" should {
        "reject the request" in new TipsAssignmentResourceFSpecContext {
          Get(s"/v1/tips_assignments.list")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("location_id")
          }
        }
      }

      "with location_id parameter" should {
        "return tips assignments" in new TipsAssignmentResourceFSpecContext with Fixtures {
          Get(s"/v1/tips_assignments.list?location_id=${london.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponse[Seq[TipsAssignmentEntity]]]

            response.data.map(_.id) ==== Seq(tipsAssignment1.id, tipsAssignment3.id)

            assertResponse(
              tipsAssignment1,
              response.data.find(_.id == tipsAssignment1.id).get,
            )

            assertResponse(
              tipsAssignment3,
              response.data.find(_.id == tipsAssignment3.id).get,
            )
          }
        }

        "with handled_via filter" should {
          "return tips assignments matching the filter" in new TipsAssignmentResourceFSpecContext with Fixtures {
            Get(s"/v1/tips_assignments.list?location_id=${london.id}&handled_via=unhandled")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val response = responseAs[ApiResponse[Seq[TipsAssignmentEntity]]]
              response.data.map(_.id) ==== Seq(tipsAssignment1.id)
            }
          }
        }

        "with updated_since filter" should {
          "return tips assignments matching the filter" in new TipsAssignmentResourceFSpecContext with Fixtures {
            Get(s"/v1/tips_assignments.list?location_id=${london.id}&updated_since=2015-12-03")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val response = responseAs[ApiResponse[Seq[TipsAssignmentEntity]]]
              response.data.map(_.id) ==== Seq(tipsAssignment3.id)
            }
          }
        }
      }
    }
  }
}
