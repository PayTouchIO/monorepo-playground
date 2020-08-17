package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object VariantOptionSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val variantOptionDao = daos.variantOptionDao

  def load(
      variantOptionTypes: Seq[VariantOptionTypeRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[VariantOptionRecord]] = {

    val variantOptions = variantOptionTypes.flatMap { variantOptionType =>
      (1 to VariantOptionsPerVariantType).map { _ =>
        VariantOptionUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          productId = Some(variantOptionType.productId),
          variantOptionTypeId = Some(variantOptionType.id),
          name = Some(randomWords),
          position = Some(0),
        )
      }
    }

    variantOptionDao.bulkUpsert(variantOptions).extractRecords
  }
}
