package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object VariantOptionTypeSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val variantOptionTypeDao = daos.variantOptionTypeDao

  def load(templateProducts: Seq[ArticleRecord])(implicit user: UserRecord): Future[Seq[VariantOptionTypeRecord]] = {

    val variantOptionTypes = templateProducts.flatMap { templateProduct =>
      (1 to VariantOptionTypePerTemplate).map { _ =>
        VariantOptionTypeUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          productId = Some(templateProduct.id),
          name = Some(randomWord),
          position = Some(0),
        )
      }
    }

    variantOptionTypeDao.bulkUpsert(variantOptionTypes).extractRecords
  }
}
