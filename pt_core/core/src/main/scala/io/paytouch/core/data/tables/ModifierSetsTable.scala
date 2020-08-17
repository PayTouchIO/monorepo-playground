package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ModifierSetRecord
import io.paytouch.core.data.model.enums.ModifierSetType

class ModifierSetsTable(tag: Tag) extends SlickMerchantTable[ModifierSetRecord](tag, "modifier_sets") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def `type` = column[ModifierSetType]("type")
  def name = column[String]("name")
  def minimumOptionCount = column[Int]("minimum_option_count")
  def maximumOptionCount = column[Option[Int]]("maximum_option_count")
  def maximumSingleOptionCount = column[Option[Int]]("maximum_single_option_count")
  def hideOnReceipts = column[Boolean]("hide_on_receipts")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def optionCountProjection =
    (
      minimumOptionCount,
      maximumOptionCount,
    ).<>(
      {
        case (min, max) => ModifierOptionCount.unsafeApply(min.pipe(Minimum), max.map(Maximum))
      },
      { moc: ModifierOptionCount => ModifierOptionCount.unapply(moc).map(_.bimap(_.value, _.map(_.value))) },
    )

  def * =
    (
      id,
      merchantId,
      `type`,
      name,
      optionCountProjection,
      maximumSingleOptionCount,
      hideOnReceipts,
      createdAt,
      updatedAt,
    ).<>(ModifierSetRecord.tupled, ModifierSetRecord.unapply)
}
