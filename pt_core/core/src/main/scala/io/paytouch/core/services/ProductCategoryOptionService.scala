package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ProductCategoryOptionRecord
import io.paytouch.core.entities.CatalogCategoryOption

import scala.concurrent._

class ProductCategoryOptionService(implicit val ec: ExecutionContext, val daos: Daos) {

  type Entity = CatalogCategoryOption
  type Record = ProductCategoryOptionRecord

  protected val dao = daos.productCategoryOptionDao

  def getOptionalCategoryOptionsPerProducts(productIds: Seq[UUID]): Future[Map[UUID, Seq[Entity]]] =
    dao
      .findByProductIds(productIds)
      .map(_.groupBy(_.productId).transform((_, v) => v.map(_.catalogCategoryOption)))
}
