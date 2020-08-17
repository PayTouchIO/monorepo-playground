package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.seeds.SeedsQuantityProvider._

import scala.concurrent._

object SupplierProductSeeds extends Seeds {

  lazy val supplierProductDao = daos.supplierProductDao

  def load(
      suppliers: Seq[SupplierRecord],
      products: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[SupplierProductRecord]] = {

    val supplierProducts = products.random(ProductsWithSuppliers).flatMap { product =>
      suppliers.randomSample.map { supplier =>
        SupplierProductUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          supplierId = Some(supplier.id),
          productId = Some(product.id),
        )
      }
    }

    supplierProductDao.bulkUpsert(supplierProducts).extractRecords
  }
}
