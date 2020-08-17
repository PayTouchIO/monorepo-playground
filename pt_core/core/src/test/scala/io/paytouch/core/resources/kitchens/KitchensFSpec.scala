package io.paytouch.core.resources.kitchens

import java.util.UUID

import io.paytouch.core.data.model.KitchenRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class KitchensFSpec extends FSpec {

  abstract class KitchenResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val kitchenDao = daos.kitchenDao

    def assertResponse(entity: Kitchen, record: KitchenRecord) = {
      entity.id ==== record.id
      entity.name ==== record.name
      entity.locationId ==== record.locationId
    }

    def assertCreation(kitchenId: UUID, creation: KitchenCreation) =
      assertUpdate(kitchenId, creation.asUpdate)

    def assertUpdate(kitchenId: UUID, update: KitchenUpdate) = {
      val kitchen = kitchenDao.findById(kitchenId).await.get
      if (update.name.isDefined) update.name ==== Some(kitchen.name)
      if (update.locationId.isDefined) update.locationId ==== Some(kitchen.locationId)
    }
  }
}
