package io.paytouch.core.data.daos

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.AvailabilityItemType

import scala.concurrent.ExecutionContext

class DiscountAvailabilityDao(implicit val ec: ExecutionContext, val db: Database) extends AvailabilityDao {

  def itemType = AvailabilityItemType.Discount
}
