package io.paytouch.ordering.data.tables

import java.time.{ LocalTime, ZonedDateTime }
import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.Uri

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ WorldpayPaymentRecord, WorldpayPaymentType }
import io.paytouch.ordering.data.tables.features.SlickTable
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import shapeless.{ Generic, HNil }
import slick.lifted.Tag
import slickless._

class WorldpayPaymentsTable(tag: Tag) extends SlickTable[WorldpayPaymentRecord](tag, "worldpay_payments") {

  def id = column[UUID]("id", O.PrimaryKey)
  def objectId = column[UUID]("object_id")
  def objectType = column[WorldpayPaymentType]("object_type")

  def transactionSetupId = column[String]("transaction_setup_id")

  def successReturnUrl = column[Uri]("success_return_url")
  def failureReturnUrl = column[Uri]("failure_return_url")

  def status = column[WorldpayPaymentStatus]("status")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (id ::
      objectId ::
      objectType ::
      transactionSetupId ::
      successReturnUrl ::
      failureReturnUrl ::
      status ::
      createdAt ::
      updatedAt :: HNil).mappedWith(Generic[WorldpayPaymentRecord])

}
