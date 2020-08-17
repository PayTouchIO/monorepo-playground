package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.SupplierProductConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.SupplierProductUpdate
import io.paytouch.core.entities.{ ArticleUpdate => ArticleUpdateEntity, SupplierUpdate => SupplierUpdateEntity, _ }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.{ MainArticleValidator, SupplierValidator }

import scala.concurrent._

class SupplierProductService(implicit val ec: ExecutionContext, val daos: Daos) extends SupplierProductConversions {

  val articleValidator = new MainArticleValidator
  val supplierValidator = new SupplierValidator

  def convertToSupplierProductUpdates(
      mainProductId: UUID,
      productUpsertion: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[SupplierProductUpdate]]]] =
    productUpsertion.supplierIds match {
      case Some(supplierIds) =>
        supplierValidator.filterValidByIds(supplierIds).map { suppliers =>
          val updates = suppliers.map { supplier =>
            toSupplierProductUpdate(productId = mainProductId, supplierId = supplier.id)
          }
          Multiple.successOpt(updates)
        }
      case None => Future.successful(Multiple.empty)
    }

  def convertToSupplierProductUpdates(
      supplierId: UUID,
      supplierUpsertion: SupplierUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[SupplierProductUpdate]]]] =
    supplierUpsertion.productIds match {
      case Some(productIds) =>
        articleValidator.accessByIds(productIds).mapNested { products =>
          val updates = products.map { product =>
            toSupplierProductUpdate(productId = product.id, supplierId = supplierId)
          }
          Some(updates)
        }
      case None => Future.successful(Multiple.empty)
    }
}
