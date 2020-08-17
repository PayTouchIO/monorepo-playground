package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object DiscountSeeds extends Seeds {

  lazy val discountDao = daos.discountDao

  def load(implicit user: UserRecord): Future[Seq[DiscountRecord]] = {
    val discountIds = discountIdsPerEmail(user.email)

    val discounts = discountIds.map { discountId =>
      val `type` = genDiscountType.instance
      DiscountUpdate(
        id = Some(discountId),
        merchantId = Some(user.merchantId),
        title = Some(randomWords),
        `type` = Some(`type`),
        amount = Some(genBigDecimal.instance),
        requireManagerApproval = None,
      )
    }

    discountDao.bulkUpsert(discounts).extractRecords

  }
}
