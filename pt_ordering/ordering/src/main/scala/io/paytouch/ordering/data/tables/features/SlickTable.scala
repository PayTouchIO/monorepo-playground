package io.paytouch.ordering.data.tables.features

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.data.driver.CustomColumnMappers
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._

abstract class SlickTable[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) with CustomColumnMappers {

  def id: Rep[UUID]
  def createdAt: Rep[ZonedDateTime]
  def updatedAt: Rep[ZonedDateTime]
}

abstract class SlickStoreTable[T](tag: Tag, tableName: String) extends SlickTable[T](tag, tableName) with StoreIdColumn
