package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ModifierSetType

import scala.concurrent._

object ModifierSetProductSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val modifierSetProductDao = daos.modifierSetProductDao

  def load(
      modifierSets: Seq[ModifierSetRecord],
      products: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ModifierSetProductRecord]] = {
    val productsWithModifierSets = products.random(ProductsWithModifierSets)

    val modifierSetProducts = productsWithModifierSets.flatMap { product =>
      val addonModifiers = modifierSets.filter(_.`type` == ModifierSetType.Addon).randomAtLeast(4)
      val holdModifiers = modifierSets.filter(_.`type` == ModifierSetType.Hold).randomAtLeast(4)
      (addonModifiers ++ holdModifiers).map { modifierSet =>
        ModifierSetProductUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          modifierSetId = Some(modifierSet.id),
          productId = Some(product.id),
          position = None,
        )
      }
    }

    modifierSetProductDao.bulkUpsertByRelIds(modifierSetProducts).extractRecords
  }
}
