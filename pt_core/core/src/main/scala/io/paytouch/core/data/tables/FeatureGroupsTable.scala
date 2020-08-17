package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.FeatureGroupRecord
import io.paytouch.core.entities.MerchantFeatures

class FeatureGroupsTable(tag: Tag) extends SlickTable[FeatureGroupRecord](tag, "feature_groups") {

  def id = column[UUID]("id", O.PrimaryKey)

  def name = column[String]("name")
  def description = column[String]("name")
  def features = column[MerchantFeatures]("features")
  def allEnabled = column[Boolean]("all_enabled")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      name,
      description,
      features,
      allEnabled,
      createdAt,
      updatedAt,
    ).<>(FeatureGroupRecord.tupled, FeatureGroupRecord.unapply)
}
