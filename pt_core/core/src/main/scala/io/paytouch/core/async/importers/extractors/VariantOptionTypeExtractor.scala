package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.async.importers.{ Keys, UpdatesWithCount }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.{ ArticleUpdate, ImportRecord, VariantOptionTypeUpdate }
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait VariantOptionTypeExtractor extends Extractor {

  val variantOptionTypeDao = daos.variantOptionTypeDao

  def extractVariantOptionTypes(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[VariantOptionTypeUpdate]] = {
    logExtraction("variant option types")
    val typesByProduct = extractTypesByProduct(data)
    val extractedVOTs = MultipleExtraction.sequence(data.map { row =>
      extractVariantOptionTypesPerRow(row, typesByProduct)
    })
    val names = extractedVOTs.getOrElse(Seq.empty).flatMap(_.name)
    val productIds = data.map { row =>
      row.articleUpdateByType(ArticleType.Variant).flatMap(_.update.isVariantOfProductId)
    }
    for {
      existingVOTs <- variantOptionTypeDao.findByNamesAndMerchantId(names, importRecord.merchantId)
    } yield extractedVOTs.map { extracted =>
      val extractedWithIds = extracted
        .distinctBy(vot => (vot.name, vot.productId))
        .map { extractedVOT =>
          val existingId = existingVOTs
            .find(vot => extractedVOT.name.contains(vot.name) && extractedVOT.productId.contains(vot.productId))
            .map(_.id)
          extractedVOT.copy(id = existingId)
        }
      toUpdatesWithCount(extractedWithIds, productIds)
    }
  }

  private def extractVariantOptionTypesPerRow(
      row: EnrichedDataRow,
      typesByProduct: Map[Option[UUID], Seq[String]],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[VariantOptionTypeUpdate]] = {
    val variantOptionTypes =
      row
        .filterKeys(_ == Keys.VariantOptionType)
        .toMap
        .flatMap {
          case (_, names) =>
            val templateProductId = row.articleUpdateByType(ArticleType.Variant).flatMap(_.update.isVariantOfProductId)
            names.flatMap { name =>
              val position = typesByProduct.getOrElse(templateProductId, Seq.empty).indexOf(name)
              templateProductId.map(toVariantOptionTypeUpdate(name, position, _))
            }
        }
        .toSeq
    MultipleExtraction.success(variantOptionTypes)
  }

  private def extractTypesByProduct(data: Seq[EnrichedDataRow]): Map[Option[UUID], Seq[String]] =
    data
      .map { row =>
        val productId = row.articleUpdateByType(ArticleType.Variant).flatMap(_.update.isVariantOfProductId)
        val typeNames = row.getOrElse(Keys.VariantOptionType, Seq.empty)
        (productId, typeNames)
      }
      .groupBy { case (productId, _typeNames) => productId }
      .transform { (_, v) =>
        v.flatMap {
          case (_productId, typeNames) => typeNames
        }.distinct
      }

  private def toUpdatesWithCount(
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
      productIds: Seq[Option[UUID]],
    ): UpdatesWithCount[VariantOptionTypeUpdate] = {
    val updates = variantOptionTypes
      .map(vot => vot.copy(id = vot.id.orElse(Some(UUID.randomUUID))))
      .sortBy(vot => (productIds.indexOf(vot.productId), vot.position))

    UpdatesWithCount(
      updates = updates,
      toAdd = variantOptionTypes.count(_.id.isEmpty),
      toUpdate = variantOptionTypes.count(_.id.isDefined),
    )
  }

  private def toVariantOptionTypeUpdate(
      name: String,
      position: Int,
      productId: UUID,
    )(implicit
      importRecord: ImportRecord,
    ): VariantOptionTypeUpdate =
    VariantOptionTypeUpdate(
      id = None,
      merchantId = Some(importRecord.merchantId),
      name = Some(name),
      position = Some(position),
      productId = Some(productId),
    )
}
