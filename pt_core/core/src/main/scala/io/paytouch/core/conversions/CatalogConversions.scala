package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.core.{ Availabilities, LocationOverridesPer }
import io.paytouch.core.data.model
import io.paytouch.core.data.model.CatalogRecord
import io.paytouch.core.entities
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.entities.UserContext

trait CatalogConversions
    extends EntityConversion[CatalogRecord, entities.Catalog]
       with ModelConversion[entities.CatalogUpsertion, model.CatalogUpdate] {
  def fromRecordsAndOptionsToEntities(
      records: Seq[CatalogRecord],
      productsCountPerCatalog: Option[Map[CatalogRecord, Int]],
      categoriesCountPerCatalog: Option[Map[CatalogRecord, Int]],
      availabilitiesPerCatalog: Option[Map[UUID, Availabilities]],
      locationOverridesPerCatalog: Option[LocationOverridesPer[UUID, entities.CatalogLocation]],
    )(implicit
      user: UserContext,
    ): Seq[entities.Catalog] =
    records.map { record =>
      val availabilities: Option[Availabilities] = record.`type` match {
        case CatalogType.DefaultMenu => availabilitiesPerCatalog.map(_ => Map.empty)
        case _                       => availabilitiesPerCatalog.map(_.getOrElse(record.id, Map.empty))
      }
      val locationOverrides = locationOverridesPerCatalog.map(_.getOrElse(record.id, Map.empty))

      fromRecordAndOptionToEntity(
        record = record,
        productsCount = productsCountPerCatalog.map(_.getOrElse(record, 0)),
        categoriesCount = categoriesCountPerCatalog.map(_.getOrElse(record, 0)),
        availabilities = availabilities,
        locationOverrides = locationOverrides,
      )
    }

  def fromRecordAndOptionToEntity(
      record: CatalogRecord,
      productsCount: Option[Int] = None,
      categoriesCount: Option[Int] = None,
      availabilities: Option[Availabilities] = None,
      locationOverrides: Option[Map[UUID, entities.CatalogLocation]] = None,
    ): entities.Catalog =
    entities.Catalog(
      id = record.id,
      name = record.name,
      `type` = record.`type`,
      productsCount = productsCount,
      categoriesCount = categoriesCount,
      availabilities = availabilities,
      locationOverrides = locationOverrides,
    )

  def fromRecordToEntity(record: CatalogRecord)(implicit user: UserContext): entities.Catalog =
    fromRecordAndOptionToEntity(record)

  def fromUpsertionToUpdate(
      id: UUID,
      update: entities.CatalogUpsertion,
    )(implicit
      user: UserContext,
    ): model.CatalogUpdate =
    model.CatalogUpdate(
      id = id.some,
      merchantId = user.merchantId.some,
      name = update.name,
      `type` = update.`type`,
    )
}
