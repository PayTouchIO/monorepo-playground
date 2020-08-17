package io.paytouch.seeds

import io.paytouch.core.data.model.{ CategoryRecord, CategoryUpdate, UserRecord }
import io.paytouch.core.entities.ResettableString

import scala.concurrent._

object SubcategorySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val systemCategoryDao = daos.systemCategoryDao

  def load(categories: Seq[CategoryRecord])(implicit user: UserRecord): Future[Seq[CategoryRecord]] = {

    val subcategories = categories.random(TotCategoriesWithSubcategories).flatMap { category =>
      val subcategoryIds = (1 to SubcategoriesPerCategory).map(idx => s"Subcategory ${category.id} $idx".toUUID)
      subcategoryIds.zipWithIndex.map {
        case (subcategoryId, idx) =>
          CategoryUpdate(
            id = Some(subcategoryId),
            merchantId = Some(user.merchantId),
            parentCategoryId = Some(category.id),
            catalogId = None,
            name = Some(randomWords),
            description = randomWords(n = 10, allCapitalized = false),
            avatarBgColor = genColor.instance,
            position = Some(idx),
          )
      }
    }
    systemCategoryDao.bulkUpsert(subcategories).extractRecords
  }
}
