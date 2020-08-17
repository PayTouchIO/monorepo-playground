package io.paytouch.core.resources.timeoffcards

import java.util.UUID

import io.paytouch.core.data.model.TimeOffCardRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class TimeOffCardsFSpec extends FSpec {

  abstract class TimeOffCardResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val locationDao = daos.locationDao
    val timeOffCardDao = daos.timeOffCardDao
    val userDao = daos.userDao

    def assertResponseById(recordId: UUID, entity: TimeOffCard) = {
      val record = timeOffCardDao.findById(recordId).await.get
      assertResponse(entity, record)
    }

    def assertResponse(entity: TimeOffCard, record: TimeOffCardRecord) = {
      entity.id ==== record.id
      entity.paid ==== record.paid
      entity.`type` ==== record.`type`
      entity.notes ==== record.notes
      entity.startAt ==== record.startAt
      entity.endAt ==== record.endAt

      assertUserResponse(entity.user)
    }

    private def assertUserResponse(entity: UserInfo) = {
      val record = userDao.findById(entity.id).await.get
      entity.id ==== record.id
      entity.firstName ==== record.firstName
      entity.lastName ==== record.lastName
    }

    def assertCreation(recordId: UUID, creation: TimeOffCardCreation) =
      assertUpdate(recordId, creation.asUpdate)

    def assertUpdate(recordId: UUID, update: TimeOffCardUpdate) = {
      val record = timeOffCardDao.findById(recordId).await.get

      if (update.userId.isDefined) update.userId ==== Some(record.userId)
      if (update.`type`.isDefined) update.`type` ==== record.`type`
      if (update.paid.isDefined) update.paid ==== Some(record.paid)
      if (update.notes.isDefined) update.notes ==== record.notes
      if (update.startAt.isDefined) update.startAt.map(_.toLocalDateTime) ==== record.startAt.map(_.toLocalDateTime)
      if (update.endAt.isDefined) update.endAt.map(_.toLocalDateTime) ==== record.endAt.map(_.toLocalDateTime)
    }
  }
}
