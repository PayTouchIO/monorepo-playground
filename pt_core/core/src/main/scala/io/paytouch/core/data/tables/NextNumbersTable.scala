package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.NextNumberRecord
import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.entities.ScopeKey

class NextNumbersTable(tag: Tag) extends SlickTable[NextNumberRecord](tag, "sequence_next_numbers") {

  def id = column[UUID]("id", O.PrimaryKey)

  def scopeType = column[ScopeType]("scope_type")
  def scopeKey = column[ScopeKey]("scope_key")

  def `type` = column[NextNumberType]("type")
  def nextVal = column[Int]("next_val")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      scopeType,
      scopeKey,
      `type`,
      nextVal,
      createdAt,
      updatedAt,
    ).<>(NextNumberRecord.tupled, NextNumberRecord.unapply)
}
