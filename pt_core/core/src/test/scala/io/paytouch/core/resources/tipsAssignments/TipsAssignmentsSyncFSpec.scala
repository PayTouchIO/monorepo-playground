package io.paytouch.core.resources.tipsAssignments

import java.util.UUID

import io.paytouch.core.entities.enums.HandledVia
import io.paytouch.core.entities.{ TipsAssignment => TipsAssignmentEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TipsAssignmentsSyncFSpec extends TipsAssignmentsFSpec {
  trait Fixtures extends TipsAssignmentResourceFSpecContext {
    val newId = UUID.randomUUID

    lazy val cashDrawer = Factory.cashDrawer(user, london).create
    lazy val cashDrawerActivity = Factory.cashDrawerActivity(cashDrawer).create
    lazy val order = Factory.order(merchant, Some(london)).create

    lazy val newUpsertionBase = TipsAssignmentUpsertion(
      id = newId,
      locationId = london.id,
      orderId = Some(order.id),
      userId = Some(user.id),
      amount = BigDecimal(10),
      handledVia = HandledVia.CashDrawerActivity,
      handledViaCashDrawerActivityId = Some(cashDrawerActivity.id),
      cashDrawerActivityId = None,
      paymentType = None,
      assignedAt = genZonedDateTime.instance,
    )
  }

  "POST /v1/tips_assignments.sync" in {
    "if request has valid token" in {
      "if the tips assignment doesn't exist" should {
        "create the tips assignment" in new TipsAssignmentResourceFSpecContext with Fixtures {
          val upsertion = newUpsertionBase.copy()

          Post(s"/v1/tips_assignments.sync?tips_assignment_id=$newId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponse[TipsAssignmentEntity]]
            assertUpsertion(response.data, upsertion)
          }
        }

        "create the tips assignment if the user id doesn't exist" in new TipsAssignmentResourceFSpecContext
          with Fixtures {
          val upsertion = newUpsertionBase.copy(userId = Some(UUID.randomUUID))

          Post(s"/v1/tips_assignments.sync?tips_assignment_id=$newId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponse[TipsAssignmentEntity]]
            assertUpsertion(response.data, upsertion.copy(userId = None))
          }
        }

        "create the tips assignment if the order id doesn't exist" in new TipsAssignmentResourceFSpecContext
          with Fixtures {
          val upsertion = newUpsertionBase.copy(orderId = Some(UUID.randomUUID))

          Post(s"/v1/tips_assignments.sync?tips_assignment_id=$newId", upsertion)
            .addHeader(authorizationHeader) ~> routes ~> check {
            val response = responseAs[ApiResponse[TipsAssignmentEntity]]
            assertUpsertion(response.data, upsertion.copy(orderId = None))
          }
        }
      }
    }
  }
}
