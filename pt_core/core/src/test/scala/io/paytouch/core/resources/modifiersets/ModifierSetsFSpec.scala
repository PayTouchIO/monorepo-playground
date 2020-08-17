package io.paytouch.core.resources.modifiersets

import java.util.UUID

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.ModifierSetLocationDao
import io.paytouch.core.data.model.{ LocationRecord, ModifierOptionRecord, ModifierSetLocationRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class ModifierSetsFSpec extends FSpec {
  abstract class ModifierSetResourceFSpecContext
      extends FSpecContext
         with MultipleLocationFixtures
         with ItemLocationSupport[ModifierSetLocationDao, ModifierSetLocationRecord, ItemLocationUpdate] {
    val articleDao = daos.articleDao
    val modifierSetDao = daos.modifierSetDao
    val itemLocationDao = daos.modifierSetLocationDao
    val modifierOptionDao = daos.modifierOptionDao
    val modifierSetProductDao = daos.modifierSetProductDao

    def assertItemLocationUpdate(
        itemId: UUID,
        locationId: UUID,
        update: ItemLocationUpdate,
      ) = {
      val record = assertItemLocationExists(itemId, locationId)
      if (update.active.isDefined) update.active ==== Some(record.active)
    }

    def assertModifierOptionUpsertion(modifierSetId: UUID, modifierOption: ModifierOption) = {
      val modifierOptionDb = modifierOptionDao.findById(modifierOption.id).await.get
      modifierOptionDb.id ==== modifierOption.id
      modifierOptionDb.modifierSetId ==== modifierSetId
      modifierOptionDb.active ==== modifierOption.active
      modifierOptionDb.name ==== modifierOption.name
      modifierOptionDb.priceAmount ==== modifierOption.price.amount
      currency ==== modifierOption.price.currency
      modifierOptionDb.position ==== modifierOption.position
    }

    def assertCreation(creation: ModifierSetCreation, id: UUID) =
      assertUpdate(creation.asUpdate, id)

    def assertUpdate(upsertion: ModifierSetUpdate, id: UUID) = {
      val modifierSet = modifierSetDao.findById(id).await.get
      if (upsertion.`type`.isDefined) upsertion.`type` ==== Some(modifierSet.`type`)
      if (upsertion.name.isDefined) upsertion.name ==== Some(modifierSet.name)
      upsertion.minimumOptionCount.foreach(_ ==== modifierSet.optionCount.minimum.value)
      upsertion.maximumOptionCount.foreach(_ ==== modifierSet.optionCount.maximum.map(_.value))
      upsertion.maximumSingleOptionCount.foreach(_ ==== modifierSet.maximumSingleOptionCount)
      if (upsertion.hideOnReceipts.isDefined) upsertion.hideOnReceipts ==== Some(modifierSet.hideOnReceipts)
      assertLocationOverridesUpdate(upsertion.locationOverrides, id)
      upsertion.options.map(_.map(_.toModifierOption)).foreach(_.foreach(assertModifierOptionUpsertion(id, _)))
    }

    def assertResponse(
        entity: ModifierSet,
        productsCount: Option[Int] = None,
        locations: Option[Seq[LocationRecord]] = None,
        options: Option[Seq[ModifierOptionRecord]] = None,
        optionsMaximumCount: Option[Int] = None,
      ) = {
      val model = modifierSetDao.findById(entity.id).await.get
      entity.id ==== model.id
      entity.name ==== model.name
      entity.minimumOptionCount.foreach(_ ==== model.optionCount.minimum.value)
      entity.maximumOptionCount.foreach(_ ==== model.optionCount.maximum.map(_.value))
      entity.singleChoice.foreach(_ ==== model.optionCount.singleChoice)
      entity.force.foreach(_ ==== model.optionCount.force)
      entity.hideOnReceipts ==== model.hideOnReceipts
      if (productsCount.isDefined) entity.productsCount ==== productsCount
      if (locations.isDefined) entity.locationOverrides.map(_.keySet) ==== locations.map(_.map(_.id).toSet)

      options.foreach { options =>
        entity.options.map(_.map(_.name)) ==== options.map(_.name).some
      }

      entity.options.map(_.map(_.maximumCount)).foreach { counts =>
        counts.forall(_ ==== optionsMaximumCount)
      }
    }
  }
}
