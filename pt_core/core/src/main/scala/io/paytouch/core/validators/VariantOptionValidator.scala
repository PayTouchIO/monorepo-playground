package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, VariantOptionDao }
import io.paytouch.core.data.model.{ VariantOptionRecord, VariantOptionTypeRecord }
import io.paytouch.core.errors.{
  InvalidVariantOptionAndOptionTypePerProduct,
  InvalidVariantOptionIds,
  InvalidVariantOptionIdsPerProduct,
  NonAccessibleVariantOptionIds,
}
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class VariantOptionValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[VariantOptionRecord] {

  type Record = VariantOptionRecord
  type Dao = VariantOptionDao

  protected val dao = daos.variantOptionDao
  val validationErrorF = InvalidVariantOptionIds(_)
  val accessErrorF = NonAccessibleVariantOptionIds(_)

  val variantOptionTypeDao = daos.variantOptionTypeDao

  def validateTypeByIdsWithProductId(
      ids: Seq[UUID],
      productId: UUID,
    ): Future[ErrorsOr[Seq[VariantOptionTypeRecord]]] =
    variantOptionTypeDao.findByIds(ids).map { types =>
      val areValid = types.forall(_.productId == productId)
      if (!areValid) {
        val invalidIds = types.filterNot(_.productId == productId).map(_.id)
        Multiple.failure(InvalidVariantOptionIdsPerProduct(invalidIds, productId))
      }
      else Multiple.success(types)
    }

  def validateOptionByIdsWithTypeIdsAndProductId(
      optionIds: Seq[UUID],
      typeIds: Seq[UUID],
      productId: UUID,
    ): Future[ErrorsOr[Seq[VariantOptionRecord]]] =
    dao.findByIds(optionIds).map { variantOptions =>
      Multiple.combine(
        belongsToVariantOptionType(variantOptions, typeIds, productId),
        belongsToProduct(variantOptions, typeIds, productId),
      ) { case _ => variantOptions }
    }

  private def belongsToVariantOptionType(
      variantOptions: Seq[VariantOptionRecord],
      typeIds: Seq[UUID],
      productId: UUID,
    ): ErrorsOr[Seq[VariantOptionRecord]] = {
    val belongsToVariantOptionType = variantOptions.forall(opt => typeIds.contains(opt.variantOptionTypeId))
    if (!belongsToVariantOptionType) {
      val invalidIds = variantOptions.filterNot(opt => typeIds.contains(opt.variantOptionTypeId)).map(_.id)
      Multiple.failure(InvalidVariantOptionAndOptionTypePerProduct(invalidIds, typeIds, productId))
    }
    else Multiple.success(variantOptions)
  }

  private def belongsToProduct(
      variantOptions: Seq[VariantOptionRecord],
      typeIds: Seq[UUID],
      productId: UUID,
    ): ErrorsOr[Seq[VariantOptionRecord]] = {
    val belongsToProduct = variantOptions.forall(_.productId == productId)
    if (!belongsToProduct) {
      val invalidIds = variantOptions.filterNot(_.productId == productId).map(_.id)
      Multiple.failure(InvalidVariantOptionAndOptionTypePerProduct(invalidIds, typeIds, productId))
    }
    else Multiple.success(variantOptions)
  }
}
