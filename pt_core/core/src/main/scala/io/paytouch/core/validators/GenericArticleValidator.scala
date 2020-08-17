package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.data.Validated.{ Invalid, Valid }

import io.paytouch._

import io.paytouch.core.data.daos.GenericArticleDao
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType }
import io.paytouch.core.entities.{ UserContext, ArticleUpdate => ArticleUpdateEntity }
import io.paytouch.core.errors._
import io.paytouch.core.SequenceOfOptionIds
import io.paytouch.core.utils.{ Multiple, _ }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.features.DefaultValidator

trait GenericArticleValidator extends DefaultValidator[ArticleRecord] {
  type Record = ArticleRecord
  type Dao <: GenericArticleDao

  val scope: Option[ArticleScope]

  val validationErrorF = InvalidProductIds(_, scope)
  val accessErrorF = NonAccessibleProductIds(_, scope)

  val variantProductDao = daos.variantProductDao

  def validateByIdsWithParentId(
      productIds: Seq[UUID],
      parentId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[ArticleRecord]]] =
    validateByIds(productIds).map {
      case Valid(products) if products.forall(_.isVariantOfProductId.contains(parentId)) =>
        Multiple.success(products)
      case Valid(products) =>
        val invalidChildrenIds = products.filterNot(_.isVariantOfProductId.contains(parentId)).map(_.id)
        Multiple.failure(InvalidProductParentId(invalidChildrenIds, parentId))
      case i @ Invalid(_) => i
    }

  def validateUpsertion(
      productId: UUID,
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ArticleUpdateEntity]] = {
    val mainProductR = dao.findById(productId)
    val variantProductsR = variantProductDao.findVariantsByParentIds(Seq(productId))
    for {
      mainProduct <- mainProductR
      variantProducts <- variantProductsR
      enrichedUpdate = enrichNewVariants(update, mainProduct, variantProducts)
      upcs <- validateUpcs(productId, enrichedUpdate)
      sku <- validateSku(productId, enrichedUpdate)
      name <- validateName(productId, enrichedUpdate)
      productType <- validateProductType(productId, enrichedUpdate, mainProduct)
      variantSelections <- validateVariantSelections(enrichedUpdate)
      variantOptions <- validateVariantOptions(enrichedUpdate)
    } yield Multiple.combine(upcs, sku, name, productType, variantSelections, variantOptions) {
      case _ => enrichedUpdate
    }
  }

  private def enrichNewVariants(
      update: ArticleUpdateEntity,
      existingProduct: Option[ArticleRecord],
      existingVariants: Seq[ArticleRecord],
    )(implicit
      user: UserContext,
    ): ArticleUpdateEntity = {
    val existingVariantIds = existingVariants.map(_.id)
    val variantToChangeIds = update.variantProducts.getOrElse(Seq.empty).map(_.id)
    val newVariantIds = variantToChangeIds diff existingVariantIds
    existingProduct match {
      case Some(p) if newVariantIds.nonEmpty =>
        val variants = update.variantProducts.getOrElse(Seq.empty)
        val enrichedVariantsToAdd = variants.filter(p => newVariantIds.contains(p.id)).map(_.enrich(p, update))
        val variantsToUpdate = variants.filterNot(p => newVariantIds.contains(p.id))
        update.copy(variantProducts = Some(variantsToUpdate ++ enrichedVariantsToAdd))
      case _ => update
    }
  }

  protected def validateVariantOptions(update: ArticleUpdateEntity): Future[ErrorsOr[ArticleUpdateEntity]] =
    Future.successful {
      val productSelectionIds = update.variantProducts.getOrElse(Seq.empty).flatMap(_.optionIds).distinct
      val variantOptionIds = update.variants.getOrElse(Seq.empty).flatMap(_.options.map(_.id)).distinct
      if (productSelectionIds.size == variantOptionIds.size) Multiple.success(update)
      else {
        val invalidIds = productSelectionIds diff variantOptionIds
        Multiple.failure(InvalidVariantOptionsInProductUpsertion(invalidIds))
      }
    }

  protected def validateVariantSelections(update: ArticleUpdateEntity): Future[ErrorsOr[ArticleUpdateEntity]] =
    Future.successful {
      val productSelectionIds = update.variantProducts.getOrElse(Seq.empty).map(_.optionIds.toSet).toSet
      val optionsSets: Seq[Seq[UUID]] = update.variants.getOrElse(Seq.empty).map(_.options.map(_.id))
      val allCombinations = optionsSets.combine.toSet.map { s: Seq[UUID] => s.toSet }
      val missingCombinations = allCombinations diff productSelectionIds

      if (missingCombinations.isEmpty) Multiple.success(update)
      else Multiple.failure(InvalidVariantSelectionsInProductUpsertion(missingCombinations.toSeq.map(_.toSeq)))
    }

  protected def validateProductType(
      productId: UUID,
      update: ArticleUpdateEntity,
      mainProduct: Option[ArticleRecord],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ArticleUpdateEntity]] =
    Future.successful {
      val variantProductUpdates = update.variantProducts.getOrElse(Seq.empty)
      mainProduct match {
        case Some(p) if p.merchantId != user.merchantId =>
          Multiple.failure(validationErrorF(Seq(productId)))
        case Some(p) if p.`type`.isSimple && variantProductUpdates.nonEmpty =>
          Multiple.failure(InvalidSimpleToVariantTypeChange())
        case Some(p) if p.`type`.isStorable && variantProductUpdates.nonEmpty =>
          Multiple.failure(InvalidProductTypeWithVariantsChange())
        case _ => Multiple.success(update)
      }
    }

  protected def validateUpcs(
      productId: UUID,
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[String]]] = {
    val upcsMap: Map[UUID, String] = (update.upc.toOption.map(productId -> _) +: update
      .variantProducts
      .getOrElse(Seq.empty)
      .map(vp => vp.upc.toOption.map(vp.id -> _))).flatten.toMap
    for {
      validUpcs <- validateUpcsAreValid(upcsMap)
      duplicatedUpcs <- validateUpcsNotDuplicated(upcsMap)
      existingUpcs <- validateUpcsNotAlreadyTaken(upcsMap)
    } yield Multiple.combine(validUpcs, duplicatedUpcs, existingUpcs) { case _ => upcsMap.values.toSeq }
  }

  def isValidUpc(upc: String): Boolean = !upc.contains(" ")

  private def validateUpcsAreValid(upcsMap: Map[UUID, String]): Future[ErrorsOr[Seq[String]]] =
    Future.successful {
      val upcs = upcsMap.values.toSeq
      val invalidUpcs = upcs.filterNot(isValidUpc)
      if (invalidUpcs.nonEmpty) Multiple.failure(InvalidUpcs(invalidUpcs, upcsMap))
      else Multiple.success(upcs)
    }

  private def validateUpcsNotAlreadyTaken(
      upcsMap: Map[UUID, String],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[String]]] = {
    val upcs = upcsMap.values.toSeq
    val ids = upcsMap.keys.toSeq
    dao.findAllByUpcsAndMerchantId(user.merchantId, upcs).map { products =>
      val alreadyTakenUpcs = products.filterNot(p => ids.contains(p.id)).flatMap(_.upc)
      if (alreadyTakenUpcs.nonEmpty) Multiple.failure(AlreadyTakenUpcs(alreadyTakenUpcs, upcsMap))
      else Multiple.success(upcs)
    }
  }

  private def validateUpcsNotDuplicated(upcsMap: Map[UUID, String]): Future[ErrorsOr[Seq[String]]] =
    validateValuesNotDuplicated(upcsMap)(DuplicatedUpcs.apply)

  private def validateValuesNotDuplicated(
      valuesMap: Map[UUID, String],
    )(
      err: (Seq[String], Map[UUID, String]) => Nel[Error],
    ): Future[ErrorsOr[Seq[String]]] =
    Future.successful {
      val allValues = valuesMap.values.toSeq
      val unique = allValues.distinct
      val duplicated = allValues diff unique
      if (duplicated.nonEmpty)
        Multiple.failure(err(allValues, valuesMap))
      else
        Multiple.success(allValues)
    }

  def isValidSku(sku: String): Boolean = !sku.contains(" ")

  protected def validateSku(
      productId: UUID,
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ArticleUpdateEntity]] =
    Future.successful {
      update.sku.toOption match {
        case Some(sku) =>
          if (!isValidSku(sku)) Multiple.failure(InvalidSku(productId, sku))
          else Multiple.success(update)
        case _ => Multiple.success(update)
      }
    }

  protected def validateName(
      productId: UUID,
      update: ArticleUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[ArticleUpdateEntity]] =
    (scope, update.name) match {
      case (Some(ArticleScope.Part), Some(name)) =>
        dao.findAllByMerchantIdAndName(user.merchantId, name).map { products =>
          val takenNameProducts =
            products
              .filter(p => p.scope == ArticleScope.Part)
              .filterNot(p => p.`type` == ArticleType.Variant)
              .filterNot(p => p.id == productId)
          if (takenNameProducts.nonEmpty) Multiple.failure(AlreadyTakenName(productId, name))
          else Multiple.success(update)
        }
      case _ => Future.successful(Multiple.success(update))
    }

  def validateStorable(accessibleProducts: Seq[ArticleRecord]): Future[ErrorsOr[Seq[ArticleRecord]]] =
    Future.successful {
      val nonStorables = accessibleProducts.filterNot(_.`type`.isStorable)
      if (nonStorables.isEmpty) Multiple.success(accessibleProducts)
      else Multiple.failure(UnexpectedNonStorableIds(nonStorables.map(_.id)))
    }

  def validateStorableByIds(ids: Seq[UUID])(implicit user: UserContext): Future[ErrorsOr[Seq[ArticleRecord]]] =
    for {
      accessibleProducts <- accessByIds(ids)
      validateStorable <- validateStorable(accessibleProducts.toList.toSeq.flatten)
    } yield Multiple.combine(accessibleProducts, validateStorable) { case (accessibleProds, _) => accessibleProds }

}
