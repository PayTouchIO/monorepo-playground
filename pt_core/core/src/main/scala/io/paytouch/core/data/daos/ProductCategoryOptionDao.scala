package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickDefaultUpsertDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductCategoryOptionRecord, ProductCategoryOptionUpdate }
import io.paytouch.core.data.tables.ProductCategoryOptionsTable
import io.paytouch.core.entities.CatalogCategoryOptionWithProductId

import scala.concurrent.ExecutionContext

class ProductCategoryOptionDao(
    productCategoryDao: => ProductCategoryDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickDefaultUpsertDao {

  type Record = ProductCategoryOptionRecord
  type Update = ProductCategoryOptionUpdate
  type Table = ProductCategoryOptionsTable

  val table = TableQuery[Table]

  def findByProductCategoryIds(ids: Seq[UUID]) =
    run(table.filter(_.productCategoryId inSet ids).result)

  def findByProductIds(ids: Seq[UUID]) = {
    val query = table
      .join(productCategoryDao.queryFindByProductIds(ids))
      .on(_.productCategoryId === _.id)
      .map {
        case (pco, pc) =>
          (
            pc.categoryId,
            pc.productId,
            pco.deliveryEnabled,
            pco.takeAwayEnabled,
          ).<>(CatalogCategoryOptionWithProductId.tupled, CatalogCategoryOptionWithProductId.unapply)
      }
    run(query.result)
  }
}
