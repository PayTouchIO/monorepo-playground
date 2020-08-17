package io.paytouch.core.resources.modifiersets

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ModifierSetsUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[ModifierSetRecord, ModifierSetLocationRecord] {

  lazy val modifierSetLocationDao = daos.modifierSetLocationDao
  lazy val modifierSetDao = daos.modifierSetDao

  def finder(id: UUID) = modifierSetLocationDao.findById(id)

  def itemFinder(id: UUID) = modifierSetDao.findById(id)

  def namespace = "modifier_sets"

  def singular = "modifier_set"

  def itemFactory(merchant: MerchantRecord) = Factory.modifierSet(merchant).create

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: ModifierSetRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.modifierSetLocation(item, location, active = active).create
}
