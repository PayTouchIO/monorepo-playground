package io.paytouch.core.data.tables

import java.time.{ ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.DynamicSortBySupport
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ LegalDetails, MerchantContext, MerchantFeatures, MerchantSetupStep }
import io.paytouch.core.entities.enums.MerchantSetupSteps

class MerchantsTable(tag: Tag)
    extends SlickTable[MerchantRecord](tag, "merchants")
       with DynamicSortBySupport.ColumnSelector {
  def id = column[UUID]("id", O.PrimaryKey)

  def active = column[Boolean]("active")
  def businessType = column[BusinessType]("business_type")
  def businessName = column[String]("business_name")
  def restaurantType = column[RestaurantType]("restaurant_type")
  def paymentProcessor = column[PaymentProcessor]("payment_processor")
  def paymentProcessorConfig = column[PaymentProcessorConfig]("payment_processor_config")
  def currency = column[Currency]("currency")
  def mode = column[MerchantMode]("mode")
  def switchMerchantId = column[Option[UUID]]("switch_merchant_id")
  def setupSteps = column[Option[Map[MerchantSetupSteps, MerchantSetupStep]]]("setup_steps")
  def setupCompleted = column[Boolean]("setup_completed")
  def loadingStatus = column[LoadingStatus]("loading_status")
  def defaultZoneId = column[ZoneId]("default_zone_id")
  def features = column[MerchantFeatures]("features")
  def legalDetails = column[Option[LegalDetails]]("legal_details")
  def setupType = column[SetupType]("setup_type")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def merchantContext =
    (id, currency, None: Rep[Option[String]]).<>((MerchantContext.apply _).tupled, MerchantContext.unapply)

  val sortableFields: Map[String, Rep[_]] = Map(
    "business_name" -> businessName,
    "created_at" -> createdAt,
    "updated_at" -> updatedAt,
  )

  def * =
    (
      id,
      active,
      businessType,
      businessName,
      restaurantType,
      paymentProcessor,
      paymentProcessorConfig,
      currency,
      mode,
      switchMerchantId,
      setupSteps,
      setupCompleted,
      loadingStatus,
      defaultZoneId,
      features,
      legalDetails,
      setupType,
      createdAt,
      updatedAt,
    ).<>(MerchantRecord.tupled, MerchantRecord.unapply)
}
