package io.paytouch.seeds

import scala.concurrent._

import cats.implicits._

import io.paytouch._

import io.paytouch.core.data.model._
import io.paytouch.seeds.IdsProvider._

object ModifierSetSeeds extends Seeds {
  lazy val modifierSetDao = daos.modifierSetDao

  def load(implicit user: UserRecord): Future[Seq[ModifierSetRecord]] = {
    val modifierSetIds = modifierSetIdsPerEmail(user.email)

    val modifierSets = modifierSetIds.map { modifierSetId =>
      ModifierSetUpdate(
        id = Some(modifierSetId),
        merchantId = Some(user.merchantId),
        `type` = Some(genModifierSetType.instance),
        name = Some(randomWords),
        optionCount = ModifierOptionCount.fromZero.toOption,
        maximumSingleOptionCount = None,
        hideOnReceipts = Some(genBoolean.instance),
      )
    }
    modifierSetDao.bulkUpsert(modifierSets).extractRecords
  }
}
