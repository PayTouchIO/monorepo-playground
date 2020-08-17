package io.paytouch.core.resources.tipsAssignments

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.TipsAssignmentRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TipsAssignmentsDeleteFSpec extends TipsAssignmentsFSpec {
  "POST /v1/tips_assignments.delete" in {
    "if request has valid token" in {
      "if tips assignments belong to a location accessible to the current merchant" should {
        "delete tips assignments" in new TipsAssignmentResourceFSpecContext {
          def create(): TipsAssignmentRecord =
            Factory.tipsAssignment(merchant, london).create

          val tipsAssignment1 = create()
          val tipsAssignment2 = create()
          val tipsAssignment3 = create()

          val deletedIds = Seq(tipsAssignment1, tipsAssignment2).map(_.id)
          val notDeletedIds = Seq(tipsAssignment3).map(_.id)
          val allIds = deletedIds ++ notDeletedIds

          tipsAssignmentDao.findByIds(allIds).await.map(_.id) should containTheSameElementsAs(allIds)

          Post(s"/v1/tips_assignments.delete", Ids(deletedIds))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            tipsAssignmentDao.findByIds(allIds).await.map(_.id) should containTheSameElementsAs(notDeletedIds)

            deletedIds.foreach(assertTipAssignmentIsMarkedAsDeleted)
          }
        }
      }
    }
  }
}
