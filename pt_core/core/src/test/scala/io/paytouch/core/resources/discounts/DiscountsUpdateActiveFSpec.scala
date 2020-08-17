package io.paytouch.core.resources.discounts

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class DiscountsUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[DiscountRecord, DiscountLocationRecord] {

  lazy val discountLocationDao = daos.discountLocationDao
  lazy val discountDao = daos.discountDao

  def finder(id: UUID) = discountLocationDao.findById(id)

  def itemFinder(id: UUID) = discountDao.findById(id)

  def namespace = "discounts"

  def singular = "discount"

  def itemFactory(merchant: MerchantRecord) = Factory.discount(merchant).create

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: DiscountRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.discountLocation(item, location, active = active).create
}
