package io.paytouch.core.resources.tipsAssignments

import java.util.UUID

import io.paytouch.core.entities.enums.HandledVia
import io.paytouch.core.entities.{ TipsAssignment => TipsAssignmentEntity, _ }
import io.paytouch.core.data.model.TipsAssignmentRecord
import io.paytouch.core.utils._

abstract class TipsAssignmentsFSpec extends FSpec {

  abstract class TipsAssignmentResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    val tipsAssignmentDao = daos.tipsAssignmentDao

    def assertResponse(record: TipsAssignmentRecord, entity: TipsAssignmentEntity) = {
      entity.id ==== record.id
      entity.locationId ==== record.locationId
      entity.userId ==== record.userId
      entity.orderId ==== record.orderId
      entity.amount.amount ==== record.amount
      entity.handledVia ==== record.handledVia
      entity.handledViaCashDrawerActivityId ==== record.handledViaCashDrawerActivityId
      entity.assignedAt.toUtc ==== record.assignedAt.toUtc
    }

    def assertUpsertion(entity: TipsAssignmentEntity, upsertion: TipsAssignmentUpsertion) = {
      val record = tipsAssignmentDao.findById(entity.id).await.get

      upsertion.locationId ==== record.locationId
      upsertion.userId ==== record.userId
      upsertion.orderId ==== record.orderId
      upsertion.amount ==== record.amount
      upsertion.handledVia ==== record.handledVia
      upsertion.handledViaCashDrawerActivityId ==== record.handledViaCashDrawerActivityId
      upsertion.assignedAt.toUtc ==== record.assignedAt.toUtc
    }

    def assertTipAssignmentIsMarkedAsDeleted(id: UUID) = {
      val assignment = tipsAssignmentDao.findDeletedById(id).await
      assignment should beSome
      assignment.flatMap(_.deletedAt) should beSome
    }

    def assertTipAssignmentIsNotMarkedAsDeleted(id: UUID) = {
      val assignment = tipsAssignmentDao.findById(id).await
      assignment should beSome
      assignment.flatMap(_.deletedAt) should beNone
    }
  }
}
