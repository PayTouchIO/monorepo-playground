package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{
  UserContext,
  VariantOptionTypeUpsertion,
  VariantOptionUpsertion,
  VariantOption => VariantOptionEntity,
  VariantOptionType => VariantOptionTypeEntity,
}

trait VariantOptionConversions extends EntityConversion[VariantOptionRecord, VariantOptionEntity] {

  def toVariantOptionUpdates(
      mainProductId: UUID,
      variantOptionsTypes: Seq[VariantOptionTypeUpsertion],
    )(implicit
      user: UserContext,
    ): Seq[VariantOptionUpdate] =
    variantOptionsTypes.flatMap(variantOptionType =>
      toVariantOptionUpdates(mainProductId, variantOptionType.id, variantOptionType.options),
    )

  def toVariantOptionUpdates(
      mainProductId: UUID,
      variantOptionTypeId: UUID,
      variantOptions: Seq[VariantOptionUpsertion],
    )(implicit
      user: UserContext,
    ): Seq[VariantOptionUpdate] =
    variantOptions.zipWithIndex.map {
      case (variantOption, index) => toVariantOptionUpdate(mainProductId, variantOptionTypeId, variantOption, index)
    }

  def toVariantOptionUpdate(
      mainProductId: UUID,
      variantOptionTypeId: UUID,
      variantOption: VariantOptionUpsertion,
      index: Int,
    )(implicit
      user: UserContext,
    ): VariantOptionUpdate =
    VariantOptionUpdate(
      id = Some(variantOption.id),
      merchantId = Some(user.merchantId),
      productId = Some(mainProductId),
      variantOptionTypeId = Some(variantOptionTypeId),
      name = Some(variantOption.name),
      position = Some(variantOption.position.getOrElse(index)),
    )

  def toVariantOptionTypeEntities(
      variantOptionTypes: Seq[VariantOptionTypeRecord],
      variantOptions: Seq[VariantOptionRecord],
    )(implicit
      user: UserContext,
    ): Map[UUID, Seq[VariantOptionTypeEntity]] = {
    val variantOptionsPerProduct = variantOptionTypes.groupBy(_.productId)
    variantOptionsPerProduct.transform((_, vots) => vots.map(toVariantOptionTypeEntity(_, variantOptions)))
  }

  def toVariantOptionTypeEntity(
      variantOptionType: VariantOptionTypeRecord,
      variantOptions: Seq[VariantOptionRecord],
    )(implicit
      user: UserContext,
    ): VariantOptionTypeEntity = {
    val options = variantOptions.filter(_.variantOptionTypeId == variantOptionType.id).map(fromRecordToEntity)
    VariantOptionTypeEntity(
      id = variantOptionType.id,
      name = variantOptionType.name,
      position = variantOptionType.position,
      options = options,
    )
  }

  def fromRecordsToEntitiesPerProductId(
      records: Seq[VariantOptionRecord],
    )(implicit
      user: UserContext,
    ): Map[UUID, Seq[VariantOptionEntity]] =
    records
      .groupBy(_.productId)
      .transform((_, vos) => vos.map(fromRecordToEntity))

  def fromRecordToEntity(record: VariantOptionRecord)(implicit user: UserContext): VariantOptionEntity =
    VariantOptionEntity(
      id = record.id,
      name = record.name,
      position = record.position,
    )
}
