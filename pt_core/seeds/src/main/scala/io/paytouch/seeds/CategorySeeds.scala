package io.paytouch.seeds

import io.paytouch.core.data.model.{ CategoryRecord, CategoryUpdate, UserRecord }
import io.paytouch.core.entities.ResettableString
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object CategorySeeds extends Seeds {

  lazy val systemCategoryDao = daos.systemCategoryDao

  def load(implicit user: UserRecord): Future[Seq[CategoryRecord]] = {
    val categoryIds = categoryIdsPerEmail(user.email)

    val categories = categoryIds.map { categoryId =>
      CategoryUpdate(
        id = Some(categoryId),
        merchantId = Some(user.merchantId),
        parentCategoryId = None,
        catalogId = None,
        name = Some(randomWords),
        description = randomWords(n = 15, allCapitalized = false),
        avatarBgColor = genColor.instance,
        position = None,
      )
    }

    systemCategoryDao.bulkUpsert(categories).extractRecords
  }
}
