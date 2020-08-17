package io.paytouch.seeds

import io.paytouch.core.data.model.{ BrandRecord, BrandUpdate, UserRecord }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object BrandSeeds extends Seeds {

  lazy val brandDao = daos.brandDao

  def load(implicit user: UserRecord): Future[Seq[BrandRecord]] = {
    val brandIds = brandIdsPerEmail(user.email)

    val brands = brandIds.map { brandId =>
      BrandUpdate(id = Some(brandId), merchantId = Some(user.merchantId), name = Some(randomWords))
    }

    brandDao.bulkUpsert(brands).extractRecords
  }
}
