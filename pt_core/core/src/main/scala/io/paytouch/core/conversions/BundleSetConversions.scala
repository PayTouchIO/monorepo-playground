package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.upsertions.BundleSetUpsertion
import io.paytouch.core.data.model.{
  BundleOptionUpdate => BundleOptionUpdateModel,
  BundleSetUpdate => BundleSetUpdateModel,
  _,
}
import io.paytouch.core.entities.{ BundleSetUpdate => BundleSetUpdateEntity, _ }

trait BundleSetConversions {

  def toBundleSetUpsertion(
      productId: UUID,
      upsertion: BundleSetUpdateEntity,
      bundleOptionUpdates: Seq[BundleOptionUpdateModel],
    )(implicit
      user: UserContext,
    ): BundleSetUpsertion =
    BundleSetUpsertion(
      toBundleSetUpdate(productId, upsertion),
      bundleOptionUpdates,
//      upsertion.options.getOrElse(Seq.empty).map(toBundleOptionUpdate(upsertion.id, _))
    )

  def toBundleSetUpdate(
      productId: UUID,
      upsertion: BundleSetUpdateEntity,
    )(implicit
      user: UserContext,
    ): BundleSetUpdateModel =
    BundleSetUpdateModel(
      id = Some(upsertion.id),
      merchantId = Some(user.merchantId),
      bundleId = Some(productId),
      name = upsertion.name,
      position = upsertion.position,
      minQuantity = upsertion.minQuantity,
      maxQuantity = upsertion.maxQuantity,
    )

  def fromRecordsAndOptionsToEntities(
      records: Seq[BundleSetRecord],
      optionsPerBundleSet: Map[UUID, Seq[BundleOption]],
    ): Seq[BundleSet] =
    records.map { record =>
      val options = optionsPerBundleSet.getOrElse(record.id, Seq.empty)
      fromRecordAndOptionsToEntity(record, options)
    }

  def fromRecordAndOptionsToEntity(record: BundleSetRecord, options: Seq[BundleOption]): BundleSet =
    BundleSet(
      id = record.id,
      name = record.name,
      position = record.position,
      minQuantity = record.minQuantity,
      maxQuantity = record.maxQuantity,
      options = options,
    )
}
