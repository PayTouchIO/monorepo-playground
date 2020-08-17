package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ModifierOptionSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val modifierOptionDao = daos.modifierOptionDao

  def load(modifierSets: Seq[ModifierSetRecord])(implicit user: UserRecord): Future[Seq[ModifierOptionRecord]] = {

    val modifierOptions = modifierSets.flatMap { modifierSet =>
      (1 to OptionsPerModifierSet).map { idx =>
        ModifierOptionUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          modifierSetId = Some(modifierSet.id),
          name = Some(randomWords),
          priceAmount = Some(genBigDecimal.instance),
          position = Some(idx),
          active = None,
        )
      }
    }
    modifierOptionDao.bulkUpsert(modifierOptions).extractRecords
  }
}
