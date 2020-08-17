package io.paytouch.seeds

import io.paytouch.core.data.model._

object ModifierSetLocationSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val modifierSetLocationDao = daos.modifierSetLocationDao

  def load(modifierSets: Seq[ModifierSetRecord], locations: Seq[LocationRecord])(implicit user: UserRecord) = {

    val modifierSetLocations = modifierSets.flatMap { modifierSet =>
      locations.random(LocationsPerModifierSet).map { location =>
        ModifierSetLocationUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          modifierSetId = Some(modifierSet.id),
          locationId = Some(location.id),
          active = None,
        )
      }
    }
    modifierSetLocationDao.bulkUpsertByRelIds(modifierSetLocations).extractRecords
  }
}
