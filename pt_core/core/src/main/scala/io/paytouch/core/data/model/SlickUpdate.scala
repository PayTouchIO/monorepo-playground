package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.upsertions.UpsertionModel
import io.paytouch.core.utils.UtcTime

trait SlickUpdate[E <: SlickRecord] extends UpsertionModel[E] {
  def id: Option[E#Id]

  final def merge(existing: Option[E]): E = existing.fold(toRecord)(updateRecord)

  /**
    * This is horrible design and should at least be renamed to sth like
    * toRecordOnlyOnCreate or toRecordIfItDoesNotExist or sth similar.
    *
    * And it should be protected at the very least, because as it stands
    * it is being abused in SlickOneToOneToLocationDao.queryUpsert
    * and probably somewhere else as well.
    */
  def toRecord: E

  def updateRecord(record: E): E

  def now = UtcTime.now
}

trait SlickMerchantUpdate[E <: SlickRecord] extends SlickUpdate[E] {
  def merchantId: Option[UUID]
}

trait SlickProductUpdate[E <: SlickRecord] extends SlickMerchantUpdate[E] {
  def productId: Option[UUID]
}

trait SlickLocationUpdate[E <: SlickRecord] extends SlickMerchantUpdate[E] {
  def locationId: Option[UUID]
}

trait SlickSoftDeleteUpdate[E <: SlickRecord] extends SlickMerchantUpdate[E] {
  def deletedAt: Option[ZonedDateTime]
}
