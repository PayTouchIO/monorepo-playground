package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._

abstract class SlickTable[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
  def id: Rep[UUID]
  def createdAt: Rep[ZonedDateTime]
  def updatedAt: Rep[ZonedDateTime]
}

abstract class SlickMerchantTable[T](tag: Tag, tableName: String) extends SlickTable[T](tag, tableName) {
  def merchantId: Rep[UUID]
}

abstract class SlickSoftDeleteTable[T](tag: Tag, tableName: String) extends SlickMerchantTable[T](tag, tableName) {
  def deletedAt: Rep[Option[ZonedDateTime]]
}
