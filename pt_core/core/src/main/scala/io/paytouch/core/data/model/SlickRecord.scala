package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReceivingObjectStatus

trait SlickId {
  final type Id = UUID

  def id: Id
}

trait SlickRecord extends SlickId {
  def createdAt: ZonedDateTime

  def updatedAt: ZonedDateTime
}

trait SlickMerchantRecord extends SlickRecord {
  def merchantId: UUID
}

trait SlickProductRecord extends SlickMerchantRecord {
  def productId: UUID
}

trait SlickItemLocationRecord extends SlickMerchantRecord {
  def itemId: UUID
  def locationId: UUID
}

trait SlickToggleableRecord extends SlickMerchantRecord {
  def active: Boolean
}

trait SlickOneToOneWithLocationRecord extends SlickMerchantRecord {
  def locationId: UUID
}

trait SlickSoftDeleteRecord extends SlickMerchantRecord {
  def deletedAt: Option[ZonedDateTime]
}

trait SlickOrderItemRelationRecord extends SlickMerchantRecord {
  def orderItemId: UUID
}

trait SlickReceivingObjectRecord extends SlickMerchantRecord {
  def status: ReceivingObjectStatus
}
