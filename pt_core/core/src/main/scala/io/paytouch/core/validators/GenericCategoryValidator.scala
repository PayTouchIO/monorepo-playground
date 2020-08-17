package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.{ Daos, GenericCategoryDao }
import io.paytouch.core.data.model.CategoryRecord
import io.paytouch.core.entities.{ UserContext, CategoryUpdate => CategoryUpdateEntity }
import io.paytouch.core.errors.{ InvalidCategoryIds, InvalidSubcategoryIds, NonAccessibleCategoryIds }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

trait GenericCategoryValidator extends DefaultValidator[CategoryRecord] {

  implicit def ec: ExecutionContext
  implicit def daos: Daos

  type Record = CategoryRecord
  type Dao <: GenericCategoryDao
  type Update = CategoryUpdateEntity

  protected def dao: Dao
  val validationErrorF = InvalidCategoryIds(_)
  val accessErrorF = NonAccessibleCategoryIds(_)

  def ptOrderingClient: PtOrderingClient

  val catalogValidator = new CatalogValidator(ptOrderingClient)

  def validateUpsertion(id: UUID, upsertion: Update)(implicit user: UserContext): Future[ErrorsOr[Update]] = {
    val subcategoryIds = upsertion.subcategories.map(_.id)
    for {
      validCategory <- validateByIdsWithParentId(Seq(id), upsertion.parentCategoryId)
      validCatalogs <- catalogValidator.accessOneByOptId(upsertion.catalogId)
      validSubcategories <- validateByIdsWithParentId(subcategoryIds, id)
    } yield Multiple.combine(validCategory, validSubcategories, validCatalogs) { case _ => upsertion }
  }

  private def validateByIdsWithParentId(
      categoryIds: Seq[UUID],
      parentId: Option[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[CategoryRecord]]] =
    parentId match {
      case Some(pId) => validateByIdsWithParentId(categoryIds, pId)
      case None      => validateByIds(categoryIds)
    }

  private def validateByIdsWithParentId(
      categoryIds: Seq[UUID],
      parentId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[CategoryRecord]]] =
    validateByIds(categoryIds).map {
      case Valid(subcategories) if subcategories.forall(_.parentCategoryId.contains(parentId)) =>
        Multiple.success(subcategories)
      case Valid(subcategories) =>
        val invalidSubcategoryIds = subcategories.filterNot(_.parentCategoryId.contains(parentId)).map(_.id)
        Multiple.failure(InvalidSubcategoryIds(invalidSubcategoryIds))
      case i @ Invalid(_) => i
    }
}
