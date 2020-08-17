package io.paytouch.ordering.resources.stores

import java.util.UUID

import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.entities.{ MonetaryAmount, Store, StoreCreation, StoreUpdate }
import io.paytouch.ordering.utils.{ FSpec, MultipleLocationFixtures }

abstract class StoresFSpec extends FSpec {

  abstract class StoreResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val storeDao = daos.storeDao

    def assertResponseById(id: UUID, entity: Store) = {
      val record = storeDao.findById(id).await.get
      assertResponse(record, entity)
    }

    def assertResponse(record: StoreRecord, entity: Store) = {
      entity.id ==== record.id
      entity.locationId ==== record.locationId
      entity.merchantUrlSlug ==== merchant.urlSlug
      entity.urlSlug ==== record.urlSlug
      entity.catalogId ==== record.catalogId
      entity.active ==== record.active
      entity.description ==== record.description
      entity.heroBgColor ==== record.heroBgColor
      entity.logoImageUrls ==== record.logoImageUrls
      entity.heroImageUrls ==== record.heroImageUrls
      entity.takeOutEnabled ==== record.takeOutEnabled
      entity.takeOutStopMinsBeforeClosing ==== record.takeOutStopMinsBeforeClosing
      entity.deliveryEnabled ==== record.deliveryEnabled
      entity.deliveryMin ==== MonetaryAmount.extract(record.deliveryMinAmount)
      entity.deliveryMax ==== MonetaryAmount.extract(record.deliveryMaxAmount)
      entity.deliveryMaxDistance ==== record.deliveryMaxDistance
      entity.deliveryStopMinsBeforeClosing ==== record.deliveryStopMinsBeforeClosing
      entity.deliveryFee ==== MonetaryAmount.extract(record.deliveryFeeAmount)
    }

    def assertCreation(id: UUID, creation: StoreCreation) =
      assertUpdate(id, creation.asUpsert)

    def assertUpdate(id: UUID, update: StoreUpdate) = {
      val record = storeDao.findById(id).await.get

      id ==== record.id
      merchant.id ==== record.merchantId
      if (update.locationId.isDefined) update.locationId ==== Some(record.locationId)
      if (update.urlSlug.isDefined) update.urlSlug ==== Some(record.urlSlug)
      if (update.catalogId.isDefined) update.catalogId ==== record.catalogId
      if (update.active.isDefined) update.active ==== Some(record.active)
      if (update.description.isDefined) update.description ==== record.description
      if (update.heroBgColor.isDefined) update.heroBgColor ==== record.heroBgColor
      if (update.logoImageUrls.isDefined) update.logoImageUrls.get ==== record.logoImageUrls
      if (update.heroImageUrls.isDefined) update.heroImageUrls.get ==== record.heroImageUrls
      if (update.takeOutEnabled.isDefined) update.takeOutEnabled ==== Some(record.takeOutEnabled)
      if (update.takeOutStopMinsBeforeClosing.isDefined)
        update.takeOutStopMinsBeforeClosing ==== record.takeOutStopMinsBeforeClosing
      if (update.deliveryEnabled.isDefined) update.deliveryEnabled ==== Some(record.deliveryEnabled)
      if (update.deliveryMinAmount.isDefined) update.deliveryMinAmount ==== record.deliveryMinAmount
      if (update.deliveryMaxAmount.isDefined) update.deliveryMaxAmount ==== record.deliveryMaxAmount
      if (update.deliveryMaxDistance.isDefined) update.deliveryMaxDistance ==== record.deliveryMaxDistance
      if (update.deliveryStopMinsBeforeClosing.isDefined)
        update.deliveryStopMinsBeforeClosing ==== record.deliveryStopMinsBeforeClosing
      if (update.deliveryFeeAmount.isDefined) update.deliveryFeeAmount ==== record.deliveryFeeAmount
    }
  }
}
