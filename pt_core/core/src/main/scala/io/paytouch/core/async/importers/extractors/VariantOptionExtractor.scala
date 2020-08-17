package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.async.importers.{ Keys, UpdatesWithCount }
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait VariantOptionExtractor extends Extractor {

  val variantOptionDao = daos.variantOptionDao

  def extractVariantOptions(
      data: Seq[EnrichedDataRow],
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[VariantOptionUpdate]] = {
    logExtraction("variant options")
    val optionsByType = extractOptionsByType(data, variantOptionTypes)
    val extractedVOs = MultipleExtraction.sequence(data.map { row =>
      extractVariantOptionsPerRow(row, variantOptionTypes, optionsByType)
    })
    val names = extractedVOs.getOrElse(Seq.empty).flatMap(_.name)
    for {
      existingVOs <- variantOptionDao.findByNamesAndMerchantId(names, importRecord.merchantId)
    } yield extractedVOs.map { extracted =>
      val extractedWithIds = extracted.distinctBy(vo => (vo.name, vo.variantOptionTypeId)).map { extractedVO =>
        val existingId = existingVOs
          .find(vo =>
            extractedVO.name.contains(vo.name) &&
              extractedVO.variantOptionTypeId.contains(vo.variantOptionTypeId),
          )
          .map(_.id)
        extractedVO.copy(id = existingId)
      }
      toUpdatesWithCount(extractedWithIds, variantOptionTypes)
    }
  }

  private def extractVariantOptionsPerRow(
      row: EnrichedDataRow,
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
      optionsByType: Map[Option[UUID], Seq[String]],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[VariantOptionUpdate]] = {
    val optionNames = row.getOrElse(Keys.VariantOption, Seq.empty)
    val typeNames = row.getOrElse(Keys.VariantOptionType, Seq.empty)
    val optionAndTypeNames =
      optionNames.zipAll(typeNames, optionNames.lastOption.getOrElse(""), typeNames.lastOption.getOrElse(""))

    val extractions = optionAndTypeNames.map {
      case (optionName, typeName) =>
        val variantOptions = findVariantOptionTypesPerRow(row, variantOptionTypes)
        variantOptions.find(_.name.contains(typeName)) match {
          case Some(vot) =>
            val position = optionsByType.getOrElse(vot.id, Seq.empty).indexOf(optionName)
            MultipleExtraction.success(Seq(toVariantOptionUpdate(optionName, position, vot)))
          case None =>
            MultipleExtraction.failure(
              ValidationError(
                Some(row.rowNumber),
                s"${Keys.VariantOptionType.capitalize} not found for ${Keys.VariantOption} $optionName",
              ),
            )
        }
    }
    MultipleExtraction.sequence(extractions)
  }

  private def extractOptionsByType(
      data: Seq[EnrichedDataRow],
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
    ): Map[Option[UUID], Seq[String]] =
    data
      .map { row =>
        val optionNames = row.getOrElse(Keys.VariantOption, Seq.empty)
        val typeIds = row.getOrElse(Keys.VariantOptionType, Seq.empty).map { typeName =>
          val variantOptions = findVariantOptionTypesPerRow(row, variantOptionTypes)
          variantOptions.find(_.name.contains(typeName)) match {
            case Some(vot) => vot.id
            case None      => None
          }
        }
        optionNames.zipAll(typeIds, optionNames.lastOption.getOrElse(""), typeIds.lastOption.getOrElse(None))
      }
      .flatten
      .groupBy { case (_optionName, typeId) => typeId }
      .transform { (_, v) =>
        v.map {
          case (optionName, _typeId) => optionName
        }.distinct
      }

  private def toUpdatesWithCount(
      variantOptions: Seq[VariantOptionUpdate],
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
    ): UpdatesWithCount[VariantOptionUpdate] = {
    val typeIds = variantOptionTypes.map(_.id)
    val updates = variantOptions
      .map(vo => vo.copy(id = vo.id.orElse(Some(UUID.randomUUID))))
      .sortBy(vo => (typeIds.indexOf(vo.variantOptionTypeId), vo.position))
    UpdatesWithCount(
      updates = updates,
      toAdd = variantOptions.count(_.id.isEmpty),
      toUpdate = variantOptions.count(_.id.isDefined),
    )
  }

  private def toVariantOptionUpdate(
      name: String,
      position: Int,
      variantOptionType: VariantOptionTypeUpdate,
    )(implicit
      importRecord: ImportRecord,
    ): VariantOptionUpdate =
    VariantOptionUpdate(
      id = None,
      merchantId = Some(importRecord.merchantId),
      variantOptionType.productId,
      variantOptionTypeId = variantOptionType.id,
      name = Some(name),
      position = Some(position),
    )
}
