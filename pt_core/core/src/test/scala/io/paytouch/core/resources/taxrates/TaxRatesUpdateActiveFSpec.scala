package io.paytouch.core.resources.taxrates

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class TaxRatesUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[TaxRateRecord, TaxRateLocationRecord] {

  lazy val taxRateLocationDao = daos.taxRateLocationDao
  lazy val taxRateDao = daos.taxRateDao

  def finder(id: UUID) = taxRateLocationDao.findById(id)

  def itemFinder(id: UUID) = taxRateDao.findById(id)

  def namespace = "tax_rates"

  def singular = "tax_rate"

  def itemFactory(merchant: MerchantRecord) = Factory.taxRate(merchant).create

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: TaxRateRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.taxRateLocation(item, location, active = active).create
}
